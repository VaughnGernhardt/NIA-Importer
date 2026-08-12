package com.nia.pipeline.tools

import com.google.gson.JsonArray
import com.google.gson.JsonParser
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.util.zip.ZipInputStream

/*
 * -------------------------------------------------------------
 * NIA — MICHIGAN MEDIAN INCOME SOURCE GENERATOR
 * -------------------------------------------------------------
 *
 * This is a ONE-TIME / OCCASIONAL utility.
 *
 * It:
 *
 * 1. Reads the Census API key from:
 *
 *      data/input/census_api_key.txt
 *
 * 2. Downloads 2024 ACS 5-year median household income
 *    for every ZCTA in the United States.
 *
 * 3. Keeps Michigan ZCTAs (48000 through 49999).
 *
 * 4. Downloads the 2024 Census ZCTA Gazetteer.
 *
 * 5. Matches each Michigan ZCTA to representative
 *    latitude / longitude coordinates.
 *
 * 6. Writes:
 *
 *      data/input/median_income_source.csv
 *
 * After this file is generated, the normal NIA pipeline
 * can work completely from the local CSV.
 */

private const val ACS_YEAR =
    "2024"

private const val ACS_BASE_URL =
    "https://api.census.gov/data/2024/acs/acs5"

private const val GAZETTEER_URL =
    "https://www2.census.gov/geo/docs/maps-data/data/" +
            "gazetteer/2024_Gazetteer/" +
            "2024_Gaz_zcta_national.zip"

private const val API_KEY_FILE =
    "data/input/census_api_key.txt"

private const val OUTPUT_FILE =
    "data/input/median_income_source.csv"

private const val MICHIGAN_ZIP_MIN =
    48000

private const val MICHIGAN_ZIP_MAX =
    49999

fun main() {

    println()
    println("=========================================")
    println("NIA Michigan Income Source Generator")
    println("=========================================")
    println()

    val apiKey =
        loadApiKey()

    val httpClient =
        HttpClient
            .newBuilder()
            .followRedirects(
                HttpClient.Redirect.NORMAL
            )
            .build()

    println(
        "Downloading ACS $ACS_YEAR median household income..."
    )

    val incomeByZip =
        downloadIncomeData(
            httpClient =
                httpClient,

            apiKey =
                apiKey
        )

    println(
        "Michigan income records found: ${incomeByZip.size}"
    )

    println()

    println(
        "Downloading Census ZCTA Gazetteer..."
    )

    val coordinatesByZip =
        downloadGazetteerCoordinates(
            httpClient =
                httpClient
        )

    println(
        "Michigan ZCTA coordinates found: ${coordinatesByZip.size}"
    )

    println()

    val combinedRows =
        incomeByZip
            .mapNotNull {
                    entry ->

                val zipCode =
                    entry.key

                val income =
                    entry.value

                val coordinates =
                    coordinatesByZip[
                        zipCode
                    ]
                        ?: return@mapNotNull null

                MichiganIncomeRow(
                    zipCode =
                        zipCode,

                    medianHouseholdIncome =
                        income,

                    latitude =
                        coordinates.latitude,

                    longitude =
                        coordinates.longitude
                )
            }
            .sortedBy {
                it.zipCode
            }

    println(
        "Combined Michigan records: ${combinedRows.size}"
    )

    writeCsv(
        rows =
            combinedRows
    )

    println()
    println(
        "Generated:"
    )

    println(
        OUTPUT_FILE
    )

    println()

    println(
        "Michigan median income source generation complete."
    )

    println()
    println("=========================================")
}

private fun loadApiKey(): String {

    val file =
        File(
            API_KEY_FILE
        )

    if (
        !file.exists()
    ) {

        throw IllegalStateException(
            "Census API key file not found: $API_KEY_FILE"
        )
    }

    val key =
        file
            .readText()
            .trim()

    if (
        key.isBlank()
    ) {

        throw IllegalStateException(
            "Census API key file is empty: $API_KEY_FILE"
        )
    }

    return key
}

private fun downloadIncomeData(
    httpClient: HttpClient,
    apiKey: String
): Map<String, Int> {

    val getValue =
        URLEncoder.encode(
            "NAME,B19013_001E",
            StandardCharsets.UTF_8
        )

    val forValue =
        URLEncoder.encode(
            "zip code tabulation area:*",
            StandardCharsets.UTF_8
        )

    val encodedApiKey =
        URLEncoder.encode(
            apiKey,
            StandardCharsets.UTF_8
        )

    val requestUrl =
        "$ACS_BASE_URL" +
                "?get=$getValue" +
                "&for=$forValue" +
                "&key=$encodedApiKey"

    val request =
        HttpRequest
            .newBuilder()
            .uri(
                URI.create(
                    requestUrl
                )
            )
            .header(
                "Accept",
                "application/json"
            )
            .header(
                "User-Agent",
                "NIA-Data-Pipeline/1.0"
            )
            .GET()
            .build()

    val response =
        httpClient.send(
            request,
            HttpResponse.BodyHandlers.ofString(
                StandardCharsets.UTF_8
            )
        )

    val body =
        response
            .body()
            .trim()

    if (
        response.statusCode() !in 200..299
    ) {

        throw IllegalStateException(
            "Census ACS request failed. " +
                    "HTTP ${response.statusCode()}. " +
                    "Response: ${body.take(500)}"
        )
    }

    if (
        !body.startsWith("[")
    ) {

        throw IllegalStateException(
            "Census ACS returned a non-JSON response: " +
                    body.take(500)
        )
    }

    val root =
        JsonParser
            .parseString(
                body
            )

    if (
        !root.isJsonArray
    ) {

        throw IllegalStateException(
            "Unexpected Census ACS response."
        )
    }

    val rows =
        root.asJsonArray

    if (
        rows.size() <= 1
    ) {

        return emptyMap()
    }

    val header =
        rows[
            0
        ]
            .asJsonArray

    val incomeIndex =
        findJsonColumnIndex(
            header =
                header,

            columnName =
                "B19013_001E"
        )

    val zipIndex =
        findJsonColumnIndex(
            header =
                header,

            columnName =
                "zip code tabulation area"
        )

    if (
        incomeIndex < 0 ||
        zipIndex < 0
    ) {

        throw IllegalStateException(
            "Required Census ACS columns were not found."
        )
    }

    val results =
        mutableMapOf<String, Int>()

    for (
    rowIndex in 1 until rows.size()
    ) {

        val rowElement =
            rows[
                rowIndex
            ]

        if (
            !rowElement.isJsonArray
        ) {

            continue
        }

        val values =
            rowElement
                .asJsonArray

        if (
            values.size() <=
            maxOf(
                incomeIndex,
                zipIndex
            )
        ) {

            continue
        }

        val zipCode =
            values[
                zipIndex
            ]
                .asString
                .trim()

        if (
            !isMichiganZip(
                zipCode
            )
        ) {

            continue
        }

        val income =
            values[
                incomeIndex
            ]
                .asString
                .trim()
                .toIntOrNull()
                ?: continue

        /*
         * Census sometimes uses negative sentinel values
         * when an estimate is unavailable.
         */
        if (
            income <= 0
        ) {

            continue
        }

        results[
            zipCode
        ] =
            income
    }

    return results
}

private fun downloadGazetteerCoordinates(
    httpClient: HttpClient
): Map<String, ZipCoordinates> {

    val request =
        HttpRequest
            .newBuilder()
            .uri(
                URI.create(
                    GAZETTEER_URL
                )
            )
            .header(
                "User-Agent",
                "NIA-Data-Pipeline/1.0"
            )
            .GET()
            .build()

    val response =
        httpClient.send(
            request,
            HttpResponse.BodyHandlers.ofInputStream()
        )

    if (
        response.statusCode() !in 200..299
    ) {

        response
            .body()
            .close()

        throw IllegalStateException(
            "Census Gazetteer download failed. " +
                    "HTTP ${response.statusCode()}."
        )
    }

    val results =
        mutableMapOf<String, ZipCoordinates>()

    response
        .body()
        .use {
                inputStream ->

            ZipInputStream(
                inputStream
            ).use {
                    zipInputStream ->

                var zipEntry =
                    zipInputStream
                        .nextEntry

                while (
                    zipEntry != null
                ) {

                    if (
                        !zipEntry.isDirectory &&
                        zipEntry.name.endsWith(
                            ".txt",
                            ignoreCase =
                                true
                        )
                    ) {

                        readGazetteerEntry(
                            zipInputStream =
                                zipInputStream,

                            results =
                                results
                        )
                    }

                    zipInputStream
                        .closeEntry()

                    zipEntry =
                        zipInputStream
                            .nextEntry
                }
            }
        }

    return results
}

private fun readGazetteerEntry(
    zipInputStream: ZipInputStream,
    results: MutableMap<String, ZipCoordinates>
) {

    /*
     * Do not close this reader independently.
     *
     * Closing it would also close the underlying
     * ZipInputStream.
     */

    val reader =
        BufferedReader(
            InputStreamReader(
                zipInputStream,
                StandardCharsets.UTF_8
            )
        )

    val headerLine =
        reader
            .readLine()
            ?: return

    val headers =
        headerLine
            .split(
                '\t'
            )
            .map {
                it.trim()
            }

    val zipIndex =
        findColumnIndex(
            headers =
                headers,

            possibleNames =
                listOf(
                    "GEOID",
                    "GEOID20",
                    "GEOID_ZCTA5_20"
                )
        )

    val latitudeIndex =
        findColumnIndex(
            headers =
                headers,

            possibleNames =
                listOf(
                    "INTPTLAT",
                    "INTPTLAT20"
                )
        )

    val longitudeIndex =
        findColumnIndex(
            headers =
                headers,

            possibleNames =
                listOf(
                    "INTPTLONG",
                    "INTPTLON",
                    "INTPTLONG20",
                    "INTPTLON20"
                )
        )

    if (
        zipIndex < 0 ||
        latitudeIndex < 0 ||
        longitudeIndex < 0
    ) {

        throw IllegalStateException(
            "Unable to identify required Gazetteer columns. " +
                    "Headers: $headers"
        )
    }

    while (
        true
    ) {

        val line =
            reader
                .readLine()
                ?: break

        if (
            line.isBlank()
        ) {

            continue
        }

        val values =
            line.split(
                '\t'
            )

        val maximumRequiredIndex =
            maxOf(
                zipIndex,
                latitudeIndex,
                longitudeIndex
            )

        if (
            values.size <= maximumRequiredIndex
        ) {

            continue
        }

        val zipCode =
            values[
                zipIndex
            ]
                .trim()

        if (
            !isMichiganZip(
                zipCode
            )
        ) {

            continue
        }

        val latitude =
            values[
                latitudeIndex
            ]
                .trim()
                .toDoubleOrNull()
                ?: continue

        val longitude =
            values[
                longitudeIndex
            ]
                .trim()
                .toDoubleOrNull()
                ?: continue

        if (
            latitude !in -90.0..90.0 ||
            longitude !in -180.0..180.0
        ) {

            continue
        }

        results[
            zipCode
        ] =
            ZipCoordinates(
                latitude =
                    latitude,

                longitude =
                    longitude
            )
    }
}

private fun writeCsv(
    rows: List<MichiganIncomeRow>
) {

    val outputFile =
        File(
            OUTPUT_FILE
        )

    outputFile
        .parentFile
        ?.mkdirs()

    outputFile
        .bufferedWriter()
        .use {
                writer ->

            writer.appendLine(
                "zipCode,medianHouseholdIncome,latitude,longitude"
            )

            rows
                .forEach {
                        row ->

                    writer.append(
                        row.zipCode
                    )

                    writer.append(
                        ","
                    )

                    writer.append(
                        row
                            .medianHouseholdIncome
                            .toString()
                    )

                    writer.append(
                        ","
                    )

                    writer.append(
                        row
                            .latitude
                            .toString()
                    )

                    writer.append(
                        ","
                    )

                    writer.append(
                        row
                            .longitude
                            .toString()
                    )

                    writer.newLine()
                }
        }
}

private fun findJsonColumnIndex(
    header: JsonArray,
    columnName: String
): Int {

    for (
    index in 0 until header.size()
    ) {

        val value =
            header[
                index
            ]
                .asString

        if (
            value.equals(
                columnName,
                ignoreCase =
                    true
            )
        ) {

            return index
        }
    }

    return -1
}

private fun findColumnIndex(
    headers: List<String>,
    possibleNames: List<String>
): Int {

    possibleNames
        .forEach {
                possibleName ->

            val index =
                headers
                    .indexOfFirst {
                            header ->

                        header.equals(
                            possibleName,
                            ignoreCase =
                                true
                        )
                    }

            if (
                index >= 0
            ) {

                return index
            }
        }

    return -1
}

private fun isMichiganZip(
    zipCode: String
): Boolean {

    if (
        zipCode.length != 5
    ) {

        return false
    }

    val numericZip =
        zipCode
            .toIntOrNull()
            ?: return false

    return numericZip in
            MICHIGAN_ZIP_MIN..
            MICHIGAN_ZIP_MAX
}

private data class ZipCoordinates(

    val latitude: Double,

    val longitude: Double
)

private data class MichiganIncomeRow(

    val zipCode: String,

    val medianHouseholdIncome: Int,

    val latitude: Double,

    val longitude: Double
)