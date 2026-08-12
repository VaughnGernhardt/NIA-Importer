package com.nia.pipeline.export

import com.nia.pipeline.analytics.model.SearchIndexEntry
import com.nia.pipeline.logging.Log
import java.io.File

class SearchIndexExporter {

    fun export(
        entries: Sequence<SearchIndexEntry>
    ): ExportSummary {

        val outputDirectory = File("data/output")

        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs()
        }

        val outputFile = File(
            outputDirectory,
            "search_index.json"
        )

        var count = 0

        outputFile.bufferedWriter().use { writer ->

            writer.appendLine("[")

            var first = true

            entries.forEach { entry ->

                if (!first) {
                    writer.appendLine(",")
                }

                writer.append(
                    """
                    {
                      "incidentNumber":"${escape(entry.incidentNumber)}",
                      "offense":"${escape(entry.offense)}",
                      "neighborhood":"${escape(entry.neighborhood)}",
                      "zipCode":"${escape(entry.zipCode)}",
                      "precinct":"${escape(entry.precinct)}",
                      "councilDistrict":"${escape(entry.councilDistrict)}",
                      "latitude":${entry.latitude},
                      "longitude":${entry.longitude}
                    }
                    """.trimIndent()
                )

                first = false
                count++
            }

            writer.appendLine()
            writer.appendLine("]")
        }

        Log.logger.info {
            "Exported $count search index entries."
        }

        return ExportSummary(
            exportedFiles = 1,
            exportedRecords = count,
            exportDirectory = outputDirectory.absolutePath
        )
    }

    private fun escape(value: String): String {

        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
    }
}