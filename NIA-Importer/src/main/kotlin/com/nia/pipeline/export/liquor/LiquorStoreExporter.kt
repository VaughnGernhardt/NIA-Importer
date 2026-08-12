package com.nia.pipeline.export.liquor

import com.nia.pipeline.analytics.model.LiquorStore
import java.io.File

class LiquorStoreExporter {

    fun export(
        stores: List<LiquorStore>,
        fileName: String = "liquor_stores.json"
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

            stores.forEachIndexed { index, store ->

                append("  {")

                append("\"businessId\":\"${escape(store.businessId)}\",")
                append("\"businessName\":\"${escape(store.businessName)}\",")
                append("\"address\":\"${escape(store.address)}\",")
                append("\"city\":\"${escape(store.city)}\",")
                append("\"zipCode\":\"${escape(store.zipCode)}\",")
                append("\"latitude\":${store.latitude},")
                append("\"longitude\":${store.longitude},")
                append("\"licenseType\":\"${escape(store.licenseType)}\"")

                append("}")

                if (index < stores.lastIndex) {
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