package com.nia.pipeline.export.geographic

import com.nia.pipeline.analytics.ZipCodeStatisticsService
import com.nia.pipeline.export.ExportSummary
import com.nia.pipeline.export.JsonExporter
import com.nia.pipeline.logging.Log
import java.io.File

class ZipCodeStatisticsExporter : JsonExporter() {

    fun export(
        statistics: List<ZipCodeStatisticsService.ZipCodeStatistic>
    ): ExportSummary {

        val outputDirectory = ensureOutputDirectory(
            "data/output/analytics"
        )

        val outputFile = File(
            outputDirectory,
            "zip_code_statistics.json"
        )

        val json = buildString {

            appendLine("[")

            statistics.forEachIndexed { index, statistic ->

                append("  {")
                append("\"zipCode\":\"${escape(statistic.zipCode)}\",")
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
            "Exported ${statistics.size} ZIP code statistics."
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