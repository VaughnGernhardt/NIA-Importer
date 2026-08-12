package com.nia.pipeline.analytics

import com.nia.pipeline.analytics.model.MedianIncomeArea
import com.nia.pipeline.logging.Log
import java.io.File
import java.sql.Connection

class MedianIncomeStatisticsService(
    @Suppress("UNUSED_PARAMETER")
    private val connection: Connection
) {

    companion object {

        private const val SOURCE_FILE =
            "data/input/median_income_source.csv"
    }

    fun generate(): List<MedianIncomeArea> {

        val file =
            File(
                SOURCE_FILE
            )

        if (
            !file.exists()
        ) {

            throw IllegalStateException(
                "Median income source file not found: $SOURCE_FILE"
            )
        }

        val lines =
            file.readLines()

        if (
            lines.size <= 1
        ) {

            Log.logger.info {
                "Median income source contains no data rows."
            }

            return emptyList()
        }

        val results =
            mutableListOf<MedianIncomeArea>()

        lines
            .drop(1)
            .forEach { line ->

                if (
                    line.isBlank()
                ) {

                    return@forEach
                }

                val parts =
                    line.split(",")

                if (
                    parts.size < 4
                ) {

                    return@forEach
                }

                val zipCode =
                    parts[0]
                        .trim()
                        .removePrefix("\"")
                        .removeSuffix("\"")

                val income =
                    parts[1]
                        .trim()
                        .removePrefix("\"")
                        .removeSuffix("\"")
                        .toIntOrNull()
                        ?: return@forEach

                val latitude =
                    parts[2]
                        .trim()
                        .removePrefix("\"")
                        .removeSuffix("\"")
                        .toDoubleOrNull()
                        ?: return@forEach

                val longitude =
                    parts[3]
                        .trim()
                        .removePrefix("\"")
                        .removeSuffix("\"")
                        .toDoubleOrNull()
                        ?: return@forEach

                if (
                    zipCode.length != 5
                ) {

                    return@forEach
                }

                if (
                    income <= 0
                ) {

                    return@forEach
                }

                if (
                    latitude !in -90.0..90.0 ||
                    longitude !in -180.0..180.0
                ) {

                    return@forEach
                }

                results.add(
                    MedianIncomeArea(
                        zipCode =
                            zipCode,

                        medianHouseholdIncome =
                            income,

                        latitude =
                            latitude,

                        longitude =
                            longitude
                    )
                )
            }

        val sortedResults =
            results
                .distinctBy {
                    it.zipCode
                }
                .sortedBy {
                    it.zipCode
                }

        Log.logger.info {
            "Median income source rows loaded: ${sortedResults.size}"
        }

        return sortedResults
    }
}