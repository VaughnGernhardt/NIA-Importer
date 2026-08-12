package com.nia.pipeline.regional

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.nia.pipeline.logging.Log
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.system.measureTimeMillis

class RegionalDatasetExporter {

    private val gson =
        GsonBuilder()
            .setPrettyPrinting()
            .create()

    fun export(
        sourcePath: Path,
        outputFileName: String,
        datasetName: String = outputFileName,
        latitudeFieldNames: List<String> =
            listOf(
                "latitude",
                "lat"
            ),
        longitudeFieldNames: List<String> =
            listOf(
                "longitude",
                "lon",
                "lng"
            )
    ) {

        Log.logger.info {
            "-----------------------------------------"
        }

        Log.logger.info {
            "Regional dataset: $datasetName"
        }

        Log.logger.info {
            "Source: $sourcePath"
        }

        Log.logger.info {
            "Output file: $outputFileName"
        }

        if (
            !Files.exists(
                sourcePath
            )
        ) {

            Log.logger.error {
                "SOURCE FILE NOT FOUND: $sourcePath"
            }

            Log.logger.info {
                "-----------------------------------------"
            }

            return
        }

        var recordsRead =
            0

        var recordsExported =
            0

        var skippedMissingCoordinates =
            0

        var skippedInvalidCoordinates =
            0

        var tileCount =
            0

        val elapsedMilliseconds =
            measureTimeMillis {

                try {

                    val jsonText =
                        Files.readString(
                            sourcePath
                        )

                    val root =
                        JsonParser.parseString(
                            jsonText
                        )

                    if (
                        !root.isJsonArray
                    ) {

                        Log.logger.error {
                            "Dataset root is not a JSON array: $sourcePath"
                        }

                        return@measureTimeMillis
                    }

                    val sourceArray =
                        root.asJsonArray

                    recordsRead =
                        sourceArray.size()

                    val tileRecords =
                        linkedMapOf<
                                String,
                                MutableList<JsonObject>
                                >()

                    sourceArray
                        .forEach { element ->

                            if (
                                !element.isJsonObject
                            ) {

                                skippedMissingCoordinates++

                                return@forEach
                            }

                            val objectValue =
                                element.asJsonObject

                            val latitude =
                                readCoordinate(
                                    objectValue =
                                        objectValue,

                                    possibleNames =
                                        latitudeFieldNames
                                )

                            val longitude =
                                readCoordinate(
                                    objectValue =
                                        objectValue,

                                    possibleNames =
                                        longitudeFieldNames
                                )

                            if (
                                latitude == null ||
                                longitude == null
                            ) {

                                skippedMissingCoordinates++

                                return@forEach
                            }

                            if (
                                latitude !in -90.0..90.0 ||
                                longitude !in -180.0..180.0
                            ) {

                                skippedInvalidCoordinates++

                                return@forEach
                            }

                            val tile =
                                RegionalTileCalculator
                                    .tileForCoordinate(
                                        latitude =
                                            latitude,

                                        longitude =
                                            longitude
                                    )

                            tileRecords
                                .getOrPut(
                                    tile.id
                                ) {
                                    mutableListOf()
                                }
                                .add(
                                    objectValue
                                )

                            recordsExported++
                        }

                    tileCount =
                        tileRecords.size

                    tileRecords
                        .forEach { entry ->

                            exportTile(
                                tileId =
                                    entry.key,

                                outputFileName =
                                    outputFileName,

                                records =
                                    entry.value
                            )
                        }

                } catch (
                    exception: Exception
                ) {

                    Log.logger.error(
                        exception
                    ) {
                        "Regional dataset export failed: $datasetName"
                    }
                }
            }

        Log.logger.info {
            "Dataset summary: $datasetName"
        }

        Log.logger.info {
            "Records read: $recordsRead"
        }

        Log.logger.info {
            "Records exported: $recordsExported"
        }

        Log.logger.info {
            "Skipped - missing coordinates: $skippedMissingCoordinates"
        }

        Log.logger.info {
            "Skipped - invalid coordinates: $skippedInvalidCoordinates"
        }

        Log.logger.info {
            "Tiles created: $tileCount"
        }

        Log.logger.info {
            "Elapsed time: ${elapsedMilliseconds} ms"
        }

        Log.logger.info {
            "-----------------------------------------"
        }
    }

    fun export(
        sourcePath: String,
        outputFileName: String,
        datasetName: String = outputFileName,
        latitudeFieldNames: List<String> =
            listOf(
                "latitude",
                "lat"
            ),
        longitudeFieldNames: List<String> =
            listOf(
                "longitude",
                "lon",
                "lng"
            )
    ) {

        export(
            sourcePath =
                Paths.get(
                    sourcePath
                ),

            outputFileName =
                outputFileName,

            datasetName =
                datasetName,

            latitudeFieldNames =
                latitudeFieldNames,

            longitudeFieldNames =
                longitudeFieldNames
        )
    }

    private fun exportTile(
        tileId: String,
        outputFileName: String,
        records: List<JsonObject>
    ) {

        val outputPath =
            Paths.get(
                "data",
                "output",
                "regional",
                "tiles",
                tileId,
                outputFileName
            )

        ensureParentDirectory(
            outputPath
        )

        val array =
            JsonArray()

        records
            .forEach { record ->

                array.add(
                    record
                )
            }

        Files.writeString(
            outputPath,
            gson.toJson(
                array
            )
        )

        Log.logger.info {
            "Tile written: $tileId/$outputFileName (${records.size} records)"
        }
    }

    private fun readCoordinate(
        objectValue: JsonObject,
        possibleNames: List<String>
    ): Double? {

        possibleNames
            .forEach { fieldName ->

                if (
                    !objectValue.has(
                        fieldName
                    )
                ) {

                    return@forEach
                }

                val value =
                    objectValue
                        .get(
                            fieldName
                        )

                if (
                    value == null ||
                    !value.isJsonPrimitive
                ) {

                    return@forEach
                }

                try {

                    return value
                        .asDouble

                } catch (
                    exception: Exception
                ) {

                    return@forEach
                }
            }

        return null
    }

    private fun ensureParentDirectory(
        outputPath: Path
    ) {

        val parent =
            outputPath.parent
                ?: return

        if (
            !Files.exists(
                parent
            )
        ) {

            Files.createDirectories(
                parent
            )
        }
    }
}