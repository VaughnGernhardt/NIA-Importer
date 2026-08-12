package com.nia.pipeline.export.vacancy

import com.nia.pipeline.analytics.model.VacantProperty
import java.io.File

class VacantPropertyExporter {

    fun export(
        properties: List<VacantProperty>,
        fileName: String = "vacant_properties.json"
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

                append(
                    "\"registrationId\":\"${escape(property.registrationId)}\","
                )

                append(
                    "\"address\":\"${escape(property.address)}\","
                )

                append(
                    "\"latitude\":${property.latitude},"
                )

                append(
                    "\"longitude\":${property.longitude},"
                )

                append(
                    "\"status\":\"${escape(property.status)}\""
                )

                append("}")

                if (
                    index < properties.lastIndex
                ) {
                    append(",")
                }

                appendLine()
            }

            appendLine("]")
        }

        outputFile.writeText(
            json
        )
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