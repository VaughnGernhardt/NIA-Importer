package com.nia.pipeline.analytics

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import com.nia.pipeline.analytics.model.LiquorStore
import com.nia.pipeline.logging.Log
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.ByteArrayInputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class LiquorStoreStatisticsService {

    companion object {

        private const val LICENSING_PAGE =
            "https://www.michigan.gov/lara/bureau-list/lcc/licensing-list"

        private const val CENSUS_GEOCODER =
            "https://geocoding.geo.census.gov/geocoder/locations/onelineaddress"

        private const val TARGET_GROUP =
            "Retail - Off Premises"

        private const val TARGET_LICENSE_TYPE =
            "Specially Designated Distributor"

        private const val TARGET_STATUS =
            "Active"

        /*
         * Approximate Michigan geographic bounds.
         *
         * Covers both Upper and Lower Peninsulas.
         */
        private const val MICHIGAN_MIN_LAT =
            41.60

        private const val MICHIGAN_MAX_LAT =
            48.35

        private const val MICHIGAN_MIN_LON =
            -90.50

        private const val MICHIGAN_MAX_LON =
            -82.10

        private const val CACHE_FILE =
            "data/cache/liquor_geocodes_michigan.json"

        private const val GEOCODE_DELAY_MS =
            75L
    }

    private val gson =
        Gson()

    private val formatter =
        DataFormatter()

    private data class GeocodeResult(

        val latitude: Double,

        val longitude: Double
    )

    private data class RawStore(

        val businessId: String,

        val businessName: String,

        val address: String,

        val city: String,

        val zipCode: String,

        val licenseType: String
    )

    fun generate(): List<LiquorStore> {

        Log.logger.info {
            "Loading Michigan liquor license workbook..."
        }

        val workbookUrl =
            resolveWorkbookUrl()

        Log.logger.info {
            "Michigan liquor workbook located."
        }

        val workbookBytes =
            downloadBytes(
                workbookUrl
            )

        Log.logger.info {
            "Michigan liquor workbook downloaded."
        }

        val rawStores =
            parseWorkbook(
                workbookBytes
            )

        Log.logger.info {
            "Active Michigan SDD licenses found: ${rawStores.size}"
        }

        val geocodeCache =
            loadGeocodeCache()

        Log.logger.info {
            "Cached Michigan liquor geocodes: ${geocodeCache.size}"
        }

        val results =
            mutableListOf<LiquorStore>()

        var cacheHits =
            0

        var geocodeSuccesses =
            0

        var geocodeFailures =
            0

        rawStores
            .forEachIndexed {
                    index,
                    store ->

                Log.logger.info {
                    "Liquor store ${index + 1}/${rawStores.size}: ${store.businessName}"
                }

                val cacheKey =
                    buildCacheKey(
                        store
                    )

                val cachedCoordinates =
                    geocodeCache[
                        cacheKey
                    ]

                val coordinates =
                    if (
                        cachedCoordinates != null
                    ) {

                        cacheHits++

                        cachedCoordinates

                    } else {

                        val result =
                            geocodeAddress(
                                address =
                                    store.address
                            )

                        if (
                            result != null
                        ) {

                            geocodeSuccesses++

                            geocodeCache[
                                cacheKey
                            ] =
                                result

                            /*
                             * Save continuously so progress is
                             * preserved if a long statewide run
                             * is interrupted.
                             */
                            if (
                                geocodeSuccesses % 25 == 0
                            ) {

                                saveGeocodeCache(
                                    geocodeCache
                                )
                            }

                        } else {

                            geocodeFailures++
                        }

                        /*
                         * Be polite to the Census geocoder.
                         */
                        Thread.sleep(
                            GEOCODE_DELAY_MS
                        )

                        result
                    }

                if (
                    coordinates == null
                ) {

                    return@forEachIndexed
                }

                if (
                    !isInsideMichigan(
                        latitude =
                            coordinates.latitude,

                        longitude =
                            coordinates.longitude
                    )
                ) {

                    return@forEachIndexed
                }

                results.add(
                    LiquorStore(

                        businessId =
                            store.businessId,

                        businessName =
                            store.businessName,

                        address =
                            store.address,

                        city =
                            store.city,

                        zipCode =
                            store.zipCode,

                        latitude =
                            coordinates.latitude,

                        longitude =
                            coordinates.longitude,

                        licenseType =
                            store.licenseType
                    )
                )
            }

        saveGeocodeCache(
            geocodeCache
        )

        val finalResults =
            results
                .distinctBy {

                    it.businessId
                        .ifBlank {

                            "${it.address}|${it.businessName}"
                        }
                }
                .sortedWith(
                    compareBy<LiquorStore> {

                        it.city
                    }
                        .thenBy {

                            it.businessName
                        }
                )

        Log.logger.info {
            "Michigan liquor store generation complete."
        }

        Log.logger.info {
            "Liquor geocode cache hits: $cacheHits"
        }

        Log.logger.info {
            "Liquor new geocode successes: $geocodeSuccesses"
        }

        Log.logger.info {
            "Liquor geocode failures: $geocodeFailures"
        }

        Log.logger.info {
            "Michigan liquor stores exported: ${finalResults.size}"
        }

        return finalResults
    }

    private fun resolveWorkbookUrl(): String {

        val html =
            downloadText(
                LICENSING_PAGE
            )

        val regex =
            Regex(
                """href=["']([^"']*Master-License-List\.xlsx[^"']*)["']""",
                RegexOption.IGNORE_CASE
            )

        val match =
            regex.find(
                html
            )
                ?: throw IllegalStateException(
                    "Unable to locate Michigan liquor license workbook."
                )

        var url =
            match
                .groupValues[
                1
            ]
                .replace(
                    "&amp;",
                    "&"
                )

        if (
            url.startsWith(
                "/"
            )
        ) {

            url =
                "https://www.michigan.gov$url"
        }

        return url
    }

    private fun parseWorkbook(
        workbookBytes: ByteArray
    ): List<RawStore> {

        val results =
            mutableListOf<RawStore>()

        WorkbookFactory
            .create(
                ByteArrayInputStream(
                    workbookBytes
                )
            )
            .use {
                    workbook ->

                val sheet =
                    workbook
                        .getSheetAt(
                            0
                        )

                val headerRow =
                    sheet
                        .getRow(
                            1
                        )
                        ?: throw IllegalStateException(
                            "Michigan liquor workbook header row not found."
                        )

                val headers =
                    mutableMapOf<String, Int>()

                headerRow
                    .forEach {
                            cell ->

                        val header =
                            formatter
                                .formatCellValue(
                                    cell
                                )
                                .trim()

                        if (
                            header.isNotBlank()
                        ) {

                            headers[
                                header
                            ] =
                                cell.columnIndex
                        }
                    }

                fun column(
                    name: String
                ): Int {

                    return headers[
                        name
                    ]
                        ?: throw IllegalStateException(
                            "Required liquor workbook column not found: $name"
                        )
                }

                val businessIdColumn =
                    column(
                        "LARA Business ID"
                    )

                val accountNameColumn =
                    column(
                        "Account Name"
                    )

                val dbaColumn =
                    column(
                        "DBA"
                    )

                val lguColumn =
                    column(
                        "Current LGU: LGU Name"
                    )

                val addressColumn =
                    column(
                        "Address"
                    )

                val groupColumn =
                    column(
                        "Group"
                    )

                val typeColumn =
                    column(
                        "Type"
                    )

                val statusColumn =
                    column(
                        "Status"
                    )

                for (
                rowIndex in
                2..sheet.lastRowNum
                ) {

                    val row =
                        sheet
                            .getRow(
                                rowIndex
                            )
                            ?: continue

                    fun value(
                        columnIndex: Int
                    ): String {

                        return formatter
                            .formatCellValue(
                                row
                                    .getCell(
                                        columnIndex
                                    )
                            )
                            .trim()
                    }

                    val group =
                        value(
                            groupColumn
                        )

                    val licenseType =
                        value(
                            typeColumn
                        )

                    val status =
                        value(
                            statusColumn
                        )

                    /*
                     * Statewide filtering:
                     *
                     * No LGU filter here.
                     */
                    if (
                        !group.equals(
                            TARGET_GROUP,
                            ignoreCase =
                                true
                        )
                    ) {

                        continue
                    }

                    if (
                        !licenseType.equals(
                            TARGET_LICENSE_TYPE,
                            ignoreCase =
                                true
                        )
                    ) {

                        continue
                    }

                    if (
                        !status.equals(
                            TARGET_STATUS,
                            ignoreCase =
                                true
                        )
                    ) {

                        continue
                    }

                    val businessId =
                        value(
                            businessIdColumn
                        )

                    val accountName =
                        value(
                            accountNameColumn
                        )

                    val dba =
                        value(
                            dbaColumn
                        )

                    val lgu =
                        value(
                            lguColumn
                        )

                    val address =
                        cleanAddress(
                            value(
                                addressColumn
                            )
                        )

                    if (
                        address.isBlank()
                    ) {

                        continue
                    }

                    val businessName =
                        if (
                            dba.isNotBlank()
                        ) {

                            dba

                        } else {

                            accountName
                        }

                    val city =
                        normalizeLguName(
                            lgu
                        )

                    val zipCode =
                        extractZipCode(
                            address
                        )

                    results.add(
                        RawStore(

                            businessId =
                                businessId,

                            businessName =
                                businessName,

                            address =
                                address,

                            city =
                                city,

                            zipCode =
                                zipCode,

                            licenseType =
                                licenseType
                        )
                    )
                }
            }

        return results
            .distinctBy {

                it.businessId
                    .ifBlank {

                        "${it.address}|${it.businessName}"
                    }
            }
    }

    private fun geocodeAddress(
        address: String
    ): GeocodeResult? {

        val encodedAddress =
            URLEncoder.encode(
                address,
                StandardCharsets.UTF_8
            )

        val url =
            "$CENSUS_GEOCODER" +
                    "?address=$encodedAddress" +
                    "&benchmark=Public_AR_Current" +
                    "&format=json"

        val response =
            try {

                downloadText(
                    url
                )

            } catch (
                exception: Exception
            ) {

                Log.logger.warn {
                    "Liquor geocode request failed for: $address"
                }

                return null
            }

        return try {

            val root =
                JsonParser
                    .parseString(
                        response
                    )
                    .asJsonObject

            val result =
                root
                    .getAsJsonObject(
                        "result"
                    )
                    ?: return null

            val matches =
                result
                    .getAsJsonArray(
                        "addressMatches"
                    )
                    ?: return null

            if (
                matches.size() == 0
            ) {

                return null
            }

            val firstMatch =
                matches[
                    0
                ]
                    .asJsonObject

            val coordinates =
                firstMatch
                    .getAsJsonObject(
                        "coordinates"
                    )
                    ?: return null

            val longitude =
                coordinates
                    .get(
                        "x"
                    )
                    ?.takeUnless {
                        it.isJsonNull
                    }
                    ?.asDouble
                    ?: return null

            val latitude =
                coordinates
                    .get(
                        "y"
                    )
                    ?.takeUnless {
                        it.isJsonNull
                    }
                    ?.asDouble
                    ?: return null

            if (
                !isInsideMichigan(
                    latitude =
                        latitude,

                    longitude =
                        longitude
                )
            ) {

                return null
            }

            GeocodeResult(
                latitude =
                    latitude,

                longitude =
                    longitude
            )

        } catch (
            exception: Exception
        ) {

            null
        }
    }

    private fun buildCacheKey(
        store: RawStore
    ): String {

        return store
            .address
            .trim()
            .uppercase()
    }

    private fun loadGeocodeCache():
            MutableMap<String, GeocodeResult> {

        val file =
            File(
                CACHE_FILE
            )

        if (
            !file.exists()
        ) {

            return mutableMapOf()
        }

        return try {

            val type =
                object :
                    TypeToken<
                            MutableMap<
                                    String,
                                    GeocodeResult
                                    >
                            >() {}
                    .type

            gson.fromJson<
                    MutableMap<
                            String,
                            GeocodeResult
                            >
                    >(
                file.readText(),
                type
            )
                ?: mutableMapOf()

        } catch (
            exception: Exception
        ) {

            mutableMapOf()
        }
    }

    private fun saveGeocodeCache(
        cache: Map<String, GeocodeResult>
    ) {

        val file =
            File(
                CACHE_FILE
            )

        file
            .parentFile
            ?.mkdirs()

        file.writeText(
            gson.toJson(
                cache
            )
        )
    }

    private fun cleanAddress(
        address: String
    ): String {

        return address
            .replace(
                Regex(
                    """\s+United States\s*$""",
                    RegexOption.IGNORE_CASE
                ),
                ""
            )
            .replace(
                Regex(
                    """\s+"""
                ),
                " "
            )
            .trim()
    }

    private fun normalizeLguName(
        lgu: String
    ): String {

        if (
            lgu.isBlank()
        ) {

            return ""
        }

        return lgu
            .replace(
                Regex(
                    """\s+(CITY|TOWNSHIP|VILLAGE)$""",
                    RegexOption.IGNORE_CASE
                ),
                ""
            )
            .trim()
            .lowercase()
            .split(
                Regex(
                    """\s+"""
                )
            )
            .joinToString(
                " "
            ) {
                    word ->

                word
                    .replaceFirstChar {
                            character ->

                        character.uppercase()
                    }
            }
    }

    private fun extractZipCode(
        address: String
    ): String {

        val matches =
            Regex(
                """\b4[89]\d{3}(?:-\d{4})?\b"""
            )
                .findAll(
                    address
                )
                .toList()

        return matches
            .lastOrNull()
            ?.value
            ?.take(
                5
            )
            ?: ""
    }

    private fun isInsideMichigan(
        latitude: Double,
        longitude: Double
    ): Boolean {

        return latitude in
                MICHIGAN_MIN_LAT..MICHIGAN_MAX_LAT &&
                longitude in
                MICHIGAN_MIN_LON..MICHIGAN_MAX_LON
    }

    private fun downloadText(
        url: String
    ): String {

        val connection =
            URI(
                url
            )
                .toURL()
                .openConnection() as
                    HttpURLConnection

        connection.requestMethod =
            "GET"

        connection.connectTimeout =
            30_000

        connection.readTimeout =
            30_000

        connection.instanceFollowRedirects =
            true

        connection.setRequestProperty(
            "User-Agent",
            "Mozilla/5.0 NIA-Importer"
        )

        connection.setRequestProperty(
            "Accept",
            "application/json,text/html,*/*"
        )

        try {

            val responseCode =
                connection.responseCode

            if (
                responseCode !in
                200..299
            ) {

                val errorText =
                    connection
                        .errorStream
                        ?.bufferedReader()
                        ?.use {
                            it.readText()
                        }
                        ?: ""

                throw IllegalStateException(
                    "HTTP request failed with $responseCode for $url. " +
                            errorText.take(
                                500
                            )
                )
            }

            return connection
                .inputStream
                .bufferedReader()
                .use {

                    it.readText()
                }

        } finally {

            connection.disconnect()
        }
    }

    private fun downloadBytes(
        url: String
    ): ByteArray {

        val connection =
            URI(
                url
            )
                .toURL()
                .openConnection() as
                    HttpURLConnection

        connection.requestMethod =
            "GET"

        connection.connectTimeout =
            30_000

        connection.readTimeout =
            60_000

        connection.instanceFollowRedirects =
            true

        connection.setRequestProperty(
            "User-Agent",
            "Mozilla/5.0 NIA-Importer"
        )

        try {

            val responseCode =
                connection.responseCode

            if (
                responseCode !in
                200..299
            ) {

                val errorText =
                    connection
                        .errorStream
                        ?.bufferedReader()
                        ?.use {
                            it.readText()
                        }
                        ?: ""

                throw IllegalStateException(
                    "HTTP request failed with $responseCode for $url. " +
                            errorText.take(
                                500
                            )
                )
            }

            return connection
                .inputStream
                .use {

                    it.readBytes()
                }

        } finally {

            connection.disconnect()
        }
    }
}