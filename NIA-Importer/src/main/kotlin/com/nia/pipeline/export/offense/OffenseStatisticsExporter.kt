package com.nia.pipeline.export.offense

import com.nia.pipeline.analytics.model.OffenseStatistic
import com.nia.pipeline.export.ExportSummary
import com.nia.pipeline.export.JsonExporter
import com.nia.pipeline.logging.Log
import java.io.File

class OffenseStatisticsExporter : JsonExporter() {

    fun export(
        statistics: List<OffenseStatistic>
    ): ExportSummary {

        val outputDirectory = ensureOutputDirectory(
            "data/output/analytics"
        )

        val outputFile = File(
            outputDirectory,
            "offense_statistics.json"
        )

        val json = buildString {

            appendLine("[")

            statistics.forEachIndexed { index, statistic ->

                append("  {")

                append("\"offense\":\"${escape(statistic.offense)}\",")
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
            "Exported ${statistics.size} offense statistics."
        }

        return ExportSummary(
            exportedFiles = 1,
            exportedRecords = statistics.size,
            exportDirectory = outputDirectory.absolutePath
        )
    }
}