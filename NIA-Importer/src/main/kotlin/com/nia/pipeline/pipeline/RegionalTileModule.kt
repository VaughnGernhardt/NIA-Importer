package com.nia.pipeline.pipeline

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.nia.pipeline.logging.Log
import com.nia.pipeline.regional.RegionalDatasetRegistry
import com.nia.pipeline.regional.RegionalManifestExporter
import com.nia.pipeline.regional.RegionalTileCalculator
import com.nia.pipeline.regional.RegionalTileManifestEntry
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class RegionalTileModule : AnalyticsModule {

    override fun execute() {

        Log.logger.info {
            "Generating regional tile manifest..."
        }

        val tileIds =
            linkedSetOf<String>()

        /*
         * -----------------------------------------------------
         * DISCOVER TILES FROM EXISTING DATASETS
         * -----------------------------------------------------
         */

        collectTilesFromFile(
            path =
                Paths.get(
                    "data",
                    "output",
                    "maps",
                    "vacant_properties.json"
                ),
            tileIds =
                tileIds
        )

        collectTilesFromFile(
            path =
                Paths.get(
                    "data",
                    "output",
                    "maps",
                    "section8_properties.json"
                ),
            tileIds =
                tileIds
        )

        collectTilesFromFile(
            path =
                Paths.get(
                    "data",
                    "output",
                    "maps",
                    "liquor_stores.json"
                ),
            tileIds =
                tileIds
        )

        collectTilesFromFile(
            path =
                Paths.get(
                    "data",
                    "output",
                    "maps",
                    "dollar_stores.json"
                ),
            tileIds =
                tileIds
        )

        collectTilesFromFile(
            path =
                Paths.get(
                    "data",
                    "output",
                    "maps",
                    "median_income.json"
                ),
            tileIds =
                tileIds
        )

        collectTilesFromFile(
            path =
                Paths.get(
                    "data",
                    "output",
                    "maps",
                    "crime_heatmap_grid.json"
                ),
            tileIds =
                tileIds
        )

        /*
         * -----------------------------------------------------
         * BUILD MANIFEST ENTRIES
         * -----------------------------------------------------
         */

        val entries =
            tileIds
                .mapNotNull { tileId ->

                    buildManifestEntry(
                        tileId
                    )
                }
                .sortedWith(
                    compareBy<RegionalTileManifestEntry> {
                        it.south
                    }
                        .thenBy {
                            it.west
                        }
                )

        /*
         * -----------------------------------------------------
         * EXPORT MANIFEST
         * -----------------------------------------------------
         */

        RegionalManifestExporter()
            .export(
                entries =
                    entries
            )

        /*
         * -----------------------------------------------------
         * REGIONAL DATASET REGISTRY
         * -----------------------------------------------------
         */

        RegionalDatasetRegistry()

            .register(
                name =
                    "Vacant Properties",

                sourcePath =
                    "data/output/maps/vacant_properties.json",

                outputFileName =
                    "vacant_properties.json"
            )

            .register(
                name =
                    "Section 8",

                sourcePath =
                    "data/output/maps/section8_properties.json",

                outputFileName =
                    "section8.json"
            )

            .register(
                name =
                    "Liquor Stores",

                sourcePath =
                    "data/output/maps/liquor_stores.json",

                outputFileName =
                    "liquor_stores.json"
            )

            .register(
                name =
                    "Dollar Stores",

                sourcePath =
                    "data/output/maps/dollar_stores.json",

                outputFileName =
                    "dollar_stores.json"
            )

            .register(
                name =
                    "Median Income",

                sourcePath =
                    "data/output/maps/median_income.json",

                outputFileName =
                    "median_income.json"
            )

            .register(
                name =
                    "Crime Heatmap",

                sourcePath =
                    "data/output/maps/crime_heatmap_grid.json",

                outputFileName =
                    "crime.json"
            )

            .exportAll()

        Log.logger.info {
            "Regional tile generation complete."
        }

        Log.logger.info {
            "Regional tiles discovered: ${entries.size}"
        }
    }

    private fun collectTilesFromFile(
        path: Path,
        tileIds: MutableSet<String>
    ) {

        if (
            !Files.exists(
                path
            )
        ) {

            Log.logger.info {
                "Regional tile source not found, skipping: $path"
            }

            return
        }

        Log.logger.info {
            "Scanning regional tile source: $path"
        }

        val jsonText =
            Files.readString(
                path
            )

        val root =
            JsonParser
                .parseString(
                    jsonText
                )

        when {

            root.isJsonArray -> {

                collectFromArray(
                    array =
                        root.asJsonArray,

                    tileIds =
                        tileIds
                )
            }

            root.isJsonObject -> {

                collectFromObject(
                    objectValue =
                        root.asJsonObject,

                    tileIds =
                        tileIds
                )
            }
        }
    }

    private fun collectFromArray(
        array: JsonArray,
        tileIds: MutableSet<String>
    ) {

        array.forEach { element ->

            if (
                element.isJsonObject
            ) {

                collectCoordinateFromObject(
                    objectValue =
                        element.asJsonObject,

                    tileIds =
                        tileIds
                )
            }
        }
    }

    private fun collectFromObject(
        objectValue: JsonObject,
        tileIds: MutableSet<String>
    ) {

        collectCoordinateFromObject(
            objectValue =
                objectValue,

            tileIds =
                tileIds
        )

        objectValue
            .entrySet()
            .forEach { entry ->

                val value =
                    entry.value

                when {

                    value.isJsonArray -> {

                        collectFromArray(
                            array =
                                value.asJsonArray,

                            tileIds =
                                tileIds
                        )
                    }

                    value.isJsonObject -> {

                        collectFromObject(
                            objectValue =
                                value.asJsonObject,

                            tileIds =
                                tileIds
                        )
                    }
                }
            }
    }

    private fun collectCoordinateFromObject(
        objectValue: JsonObject,
        tileIds: MutableSet<String>
    ) {

        val latitude =
            readCoordinate(
                objectValue =
                    objectValue,

                possibleNames =
                    listOf(
                        "latitude",
                        "lat"
                    )
            )
                ?: return

        val longitude =
            readCoordinate(
                objectValue =
                    objectValue,

                possibleNames =
                    listOf(
                        "longitude",
                        "lon",
                        "lng"
                    )
            )
                ?: return

        if (
            latitude !in -90.0..90.0 ||
            longitude !in -180.0..180.0
        ) {

            return
        }

        val tile =
            RegionalTileCalculator
                .tileForCoordinate(
                    latitude =
                        latitude,

                    longitude =
                        longitude
                )

        tileIds.add(
            tile.id
        )
    }

    private fun readCoordinate(
        objectValue: JsonObject,
        possibleNames: List<String>
    ): Double? {

        possibleNames
            .forEach { name ->

                if (
                    objectValue.has(
                        name
                    )
                ) {

                    val value =
                        objectValue
                            .get(
                                name
                            )

                    if (
                        value != null &&
                        value.isJsonPrimitive
                    ) {

                        try {

                            return value
                                .asDouble

                        } catch (
                            exception: Exception
                        ) {

                            return null
                        }
                    }
                }
            }

        return null
    }

    private fun buildManifestEntry(
        tileId: String
    ): RegionalTileManifestEntry? {

        val south =
            parseSouthFromTileId(
                tileId
            )
                ?: return null

        val west =
            parseWestFromTileId(
                tileId
            )
                ?: return null

        val bounds =
            RegionalTileCalculator
                .tileForCoordinate(
                    latitude =
                        south + 0.00001,

                    longitude =
                        west + 0.00001
                )

        val tileDirectory =
            "regional/tiles/${bounds.id}"

        return RegionalTileManifestEntry(

            id =
                bounds.id,

            state =
                "MI",

            county =
                null,

            south =
                bounds.south,

            west =
                bounds.west,

            north =
                bounds.north,

            east =
                bounds.east,

            crimeFile =
                "$tileDirectory/crime.json",

            vacantPropertyFile =
                "$tileDirectory/vacant_properties.json",

            medianIncomeFile =
                "$tileDirectory/median_income.json",

            section8File =
                "$tileDirectory/section8.json",

            liquorStoreFile =
                "$tileDirectory/liquor_stores.json",

            dollarStoreFile =
                "$tileDirectory/dollar_stores.json",

            noiseFile =
                null
        )
    }

    private fun parseSouthFromTileId(
        tileId: String
    ): Double? {

        val separatorIndex =
            tileId.indexOf(
                "_-"
            )

        if (
            separatorIndex <= 0
        ) {

            return null
        }

        val latitudePart =
            tileId
                .substring(
                    0,
                    separatorIndex
                )
                .replace(
                    "_",
                    "."
                )

        return latitudePart
            .toDoubleOrNull()
    }

    private fun parseWestFromTileId(
        tileId: String
    ): Double? {

        val separatorIndex =
            tileId.indexOf(
                "_-"
            )

        if (
            separatorIndex < 0
        ) {

            return null
        }

        val longitudePart =
            tileId
                .substring(
                    separatorIndex + 1
                )
                .replace(
                    "_",
                    "."
                )

        return longitudePart
            .toDoubleOrNull()
    }
}