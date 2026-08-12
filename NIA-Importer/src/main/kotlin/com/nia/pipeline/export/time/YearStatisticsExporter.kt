package com.nia.pipeline.export.time

import com.nia.pipeline.analytics.YearStatisticsService
import com.nia.pipeline.export.ExportSummary
import com.nia.pipeline.export.JsonExporter
import com.nia.pipeline.logging.Log
import java.io.File

class YearStatisticsExporter : JsonExporter() {

    fun export(
        statistics: List<YearStatisticsService.YearStatistic>
    ): ExportSummary {

        val outputDirectory = ensureOutputDirectory(
            "data/output/analytics"
        )

        val outputFile = File(
            outputDirectory,
            "year_statistics.json"
        )

        val json = buildString {

            appendLine("[")

            statistics.forEachIndexed { index, statistic ->

                append("  {")
                append("\"year\":${statistic.year},")
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
            "Exported ${statistics.size} year statistics."
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