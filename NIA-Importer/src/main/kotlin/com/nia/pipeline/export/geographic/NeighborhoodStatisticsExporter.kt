package com.nia.pipeline.export.geographic

import com.nia.pipeline.analytics.NeighborhoodStatisticsService
import com.nia.pipeline.export.ExportSummary
import com.nia.pipeline.export.JsonExporter
import com.nia.pipeline.logging.Log
import java.io.File

class NeighborhoodStatisticsExporter : JsonExporter() {

    fun export(
        statistics: List<NeighborhoodStatisticsService.NeighborhoodStatistic>
    ): ExportSummary {

        val outputDirectory = ensureOutputDirectory(
            "data/output/analytics"
        )

        val outputFile = File(
            outputDirectory,
            "neighborhood_statistics.json"
        )

        val json = buildString {

            appendLine("[")

            statistics.forEachIndexed { index, statistic ->

                append("  {")
                append("\"neighborhood\":\"${escape(statistic.neighborhood)}\",")
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
            "Exported ${statistics.size} neighborhood statistics."
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