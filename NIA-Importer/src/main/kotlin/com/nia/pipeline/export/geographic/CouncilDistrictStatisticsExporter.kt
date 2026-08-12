package com.nia.pipeline.export.geographic

import com.nia.pipeline.analytics.CouncilDistrictStatisticsService
import com.nia.pipeline.export.ExportSummary
import com.nia.pipeline.export.JsonExporter
import com.nia.pipeline.logging.Log
import java.io.File

class CouncilDistrictStatisticsExporter : JsonExporter() {

    fun export(
        statistics: List<CouncilDistrictStatisticsService.CouncilDistrictStatistic>
    ): ExportSummary {

        val outputDirectory = ensureOutputDirectory(
            "data/output/analytics"
        )

        val outputFile = File(
            outputDirectory,
            "council_district_statistics.json"
        )

        val json = buildString {

            appendLine("[")

            statistics.forEachIndexed { index, statistic ->

                append("  {")
                append(
                    "\"councilDistrict\":\"${escape(statistic.councilDistrict)}\","
                )
                append("\"incidentCount\":${statistic.incidentCount}")
                append("}")

                if (index < statistics.lastIndex) {
                    append(",")
                }

                appendLine()
            }

            appendLine("]")
        }

        outputFile.writeText(json)

        Log.logger.info {
            "Exported ${statistics.size} council district statistics."
        }

        Log.logger.info {
            "Created ${outputFile.absolutePath}"
        }

        return ExportSummary(
            exportedFiles = 1,
            exportedRecords = statistics.size,
            exportDirectory = outputDirectory.absolutePath
        )
    }
}