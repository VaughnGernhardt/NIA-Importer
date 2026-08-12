package com.nia.pipeline.regional

import java.nio.file.Paths

class RegionalSection8Exporter {

    fun export() {

        RegionalDatasetExporter()
            .export(
                sourcePath =
                    Paths.get(
                        "data",
                        "output",
                        "maps",
                        "section8_properties.json"
                    ),

                outputFileName =
                    "section8.json",

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