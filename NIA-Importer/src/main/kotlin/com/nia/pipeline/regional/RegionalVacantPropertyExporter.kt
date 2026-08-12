package com.nia.pipeline.regional

import java.nio.file.Paths

class RegionalVacantPropertyExporter {

    fun export() {

        RegionalDatasetExporter()
            .export(
                sourcePath =
                    Paths.get(
                        "data",
                        "output",
                        "maps",
                        "vacant_properties.json"
                    ),

                outputFileName =
                    "vacant_properties.json",

                latitudeFieldNames =
                    listOf(
                        "latitude",
                        "lat"
                    ),

                longitudeFieldNames =
                    listOf(
                        "longitude",
                        "lon",
                        "lng"
                    )
            )
    }
}