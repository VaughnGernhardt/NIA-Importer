package com.nia.pipeline.export.noise

import com.nia.pipeline.analytics.model.NoiseHeatmapCell
import java.io.File

class NoiseHeatmapExporter {

    fun export(
        cells: List<NoiseHeatmapCell>,
        fileName: String = "noise_heatmap_grid.json"
    ) {

        val outputDirectory =
            File("data/output/maps")

        outputDirectory.mkdirs()

        val outputFile =
            File(
                outputDirectory,
                fileName
            )

        val json = buildString {

            appendLine("[")

            cells.forEachIndexed { index, cell ->

                append("  {")

                append("\"latitude\":${cell.latitude},")

                append("\"longitude\":${cell.longitude},")

                append("\"decibels\":${cell.decibels},")

                append("\"measurementCount\":${cell.measurementCount}")

                append("}")

                if (index < cells.lastIndex) {
                    append(",")
                }

                appendLine()
            }

            appendLine("]")
        }

        outputFile.writeText(json)
    }
}