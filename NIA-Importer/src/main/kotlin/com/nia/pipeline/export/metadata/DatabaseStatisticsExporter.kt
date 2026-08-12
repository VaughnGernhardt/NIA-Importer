package com.nia.pipeline.export.metadata

import com.nia.pipeline.analytics.model.DatabaseStatistics
import com.nia.pipeline.export.ExportSummary
import com.nia.pipeline.export.JsonExporter
import com.nia.pipeline.logging.Log
import java.io.File

class DatabaseStatisticsExporter : JsonExporter() {

    fun export(
        statistics: DatabaseStatistics
    ): ExportSummary {

        val outputDirectory = ensureOutputDirectory(
            "data/output/metadata"
        )

        val outputFile = File(
            outputDirectory,
            "database_statistics.json"
        )

        val json = buildString {

            appendLine("{")
            appendLine("  \"totalIncidents\": ${statistics.totalIncidents},")
            appendLine("  \"uniqueOffenses\": ${statistics.uniqueOffenses},")
            appendLine("  \"uniqueNeighborhoods\": ${statistics.uniqueNeighborhoods},")
            appendLine("  \"uniqueZipCodes\": ${statistics.uniqueZipCodes},")
            appendLine("  \"uniquePrecincts\": ${statistics.uniquePrecincts},")
            appendLine("  \"firstCrimeYear\": ${statistics.firstCrimeYear},")
            appendLine("  \"lastCrimeYear\": ${statistics.lastCrimeYear}")
            appendLine("}")
        }

        outputFile.writeText(json)

        Log.logger.info {
            "Exported database statistics."
        }

        Log.logger.info {
            "Created ${outputFile.absolutePath}"
        }

        return ExportSummary(
            exportedFiles = 1,
            exportedRecords = 1,
            exportDirectory = outputDirectory.absolutePath
        )
    }
}