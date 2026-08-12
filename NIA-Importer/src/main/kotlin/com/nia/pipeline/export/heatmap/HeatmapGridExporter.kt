package com.nia.pipeline.export.heatmap

import com.nia.pipeline.analytics.model.HeatmapCell
import com.nia.pipeline.export.ExportSummary
import com.nia.pipeline.export.JsonExporter
import com.nia.pipeline.logging.Log
import java.io.File

class HeatmapGridExporter : JsonExporter() {

    fun export(
        cells: List<HeatmapCell>,
        fileName: String = "crime_heatmap_grid.json"
    ): ExportSummary {

        val outputDirectory = ensureOutputDirectory(
            "data/output/maps"
        )

        val outputFile = File(
            outputDirectory,
            fileName
        )

        val json = buildString {

            appendLine("[")

            cells.forEachIndexed { index, cell ->

                append("  {")

                append("\"latitude\":${cell.latitude},")

                append("\"longitude\":${cell.longitude},")

                append("\"intensity\":${cell.intensity}")

                append("}")

                if (index < cells.lastIndex) {
                    append(",")
                }

                appendLine()
            }

            appendLine("]")
        }

        outputFile.writeText(json)

        Log.logger.info {
            "Exported ${cells.size} heatmap cells to $fileName."
        }

        return ExportSummary(
            exportedFiles = 1,
            exportedRecords = cells.size,
            exportDirectory = outputDirectory.absolutePath
        )
    }
}