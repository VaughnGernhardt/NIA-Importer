package com.nia.pipeline.export.section8

import com.nia.pipeline.analytics.model.Section8Property
import java.io.File

class Section8Exporter {

    fun export(
        properties: List<Section8Property>,
        fileName: String = "section8_properties.json"
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

            properties.forEachIndexed { index, property ->

                append("  {")

                append("\"propertyId\":\"${escape(property.propertyId)}\",")

                append("\"propertyName\":\"${escape(property.propertyName)}\",")

                append("\"latitude\":${property.latitude},")

                append("\"longitude\":${property.longitude},")

                append("\"assistedUnits\":${property.assistedUnits},")

                append("\"totalUnits\":${property.totalUnits}")

                append("}")

                if (index < properties.lastIndex) {
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