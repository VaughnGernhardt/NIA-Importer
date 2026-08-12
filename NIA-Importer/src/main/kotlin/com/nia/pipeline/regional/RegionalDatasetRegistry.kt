package com.nia.pipeline.regional

import com.nia.pipeline.logging.Log
import java.nio.file.Path
import java.nio.file.Paths

class RegionalDatasetRegistry {

    data class DatasetDefinition(

        val name: String,

        val sourcePath: Path,

        val outputFileName: String,

        val latitudeFieldNames: List<String> =
            listOf(
                "latitude",
                "lat"
            ),

        val longitudeFieldNames: List<String> =
            listOf(
                "longitude",
                "lon",
                "lng"
            )
    )

    private val datasets =
        mutableListOf<DatasetDefinition>()

    /*
     * ---------------------------------------------------------
     * REGISTER DATASET
     * ---------------------------------------------------------
     */

    fun register(
        name: String,
        sourcePath: Path,
        outputFileName: String,
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
    ): RegionalDatasetRegistry {

        datasets.add(
            DatasetDefinition(
                name =
                    name,

                sourcePath =
                    sourcePath,

                outputFileName =
                    outputFileName,

                latitudeFieldNames =
                    latitudeFieldNames,

                longitudeFieldNames =
                    longitudeFieldNames
            )
        )

        return this
    }

    /*
     * ---------------------------------------------------------
     * REGISTER DATASET USING STRING PATH
     * ---------------------------------------------------------
     */

    fun register(
        name: String,
        sourcePath: String,
        outputFileName: String,
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
    ): RegionalDatasetRegistry {

        return register(
            name =
                name,

            sourcePath =
                Paths.get(
                    sourcePath
                ),

            outputFileName =
                outputFileName,

            latitudeFieldNames =
                latitudeFieldNames,

            longitudeFieldNames =
                longitudeFieldNames
        )
    }

    /*
     * ---------------------------------------------------------
     * EXPORT ALL REGISTERED DATASETS
     * ---------------------------------------------------------
     */

    fun exportAll() {

        Log.logger.info {
            "========================================="
        }

        Log.logger.info {
            "Starting regional dataset registry..."
        }

        Log.logger.info {
            "Registered regional datasets: ${datasets.size}"
        }

        Log.logger.info {
            "========================================="
        }

        val exporter =
            RegionalDatasetExporter()

        datasets
            .forEach { dataset ->

                Log.logger.info {
                    "Regional export starting: ${dataset.name}"
                }

                exporter.export(
                    sourcePath =
                        dataset.sourcePath,

                    outputFileName =
                        dataset.outputFileName,

                    latitudeFieldNames =
                        dataset.latitudeFieldNames,

                    longitudeFieldNames =
                        dataset.longitudeFieldNames
                )

                Log.logger.info {
                    "Regional export finished: ${dataset.name}"
                }
            }

        Log.logger.info {
            "========================================="
        }

        Log.logger.info {
            "Regional dataset registry complete."
        }

        Log.logger.info {
            "========================================="
        }
    }

    /*
     * ---------------------------------------------------------
     * DATASET COUNT
     * ---------------------------------------------------------
     */

    fun size(): Int {

        return datasets.size
    }

    /*
     * ---------------------------------------------------------
     * CLEAR REGISTRY
     * ---------------------------------------------------------
     *
     * Mostly useful for testing later.
     */

    fun clear() {

        datasets.clear()
    }
}