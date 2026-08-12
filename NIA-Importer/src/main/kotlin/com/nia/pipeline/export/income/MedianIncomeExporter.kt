package com.nia.pipeline.export.income

import com.nia.pipeline.analytics.model.MedianIncomeArea
import java.io.File

class MedianIncomeExporter {

    fun export(
        areas: List<MedianIncomeArea>,
        fileName: String = "median_income.json"
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

            areas.forEachIndexed { index, area ->

                append("  {")

                append("\"zipCode\":\"${escape(area.zipCode)}\",")
                append("\"medianHouseholdIncome\":${area.medianHouseholdIncome},")
                append("\"latitude\":${area.latitude},")
                append("\"longitude\":${area.longitude}")

                append("}")

                if (index < areas.lastIndex) {
                    append(",")
                }

                appendLine()
            }

            appendLine("]")
        }

        outputFile.writeText(json)
    }

    private fun escape(
        value: String
    ): String {

        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", " ")
            .replace("\r", " ")
    }
}