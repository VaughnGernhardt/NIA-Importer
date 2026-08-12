package com.nia.pipeline.export.dollar

import com.nia.pipeline.analytics.model.DollarStore
import java.io.File

class DollarStoreExporter {

    fun export(
        stores: List<DollarStore>,
        fileName: String = "dollar_stores.json"
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

                append("\"storeId\":\"${escape(store.storeId)}\",")
                append("\"storeName\":\"${escape(store.storeName)}\",")
                append("\"chain\":\"${escape(store.chain)}\",")
                append("\"address\":\"${escape(store.address)}\",")
                append("\"city\":\"${escape(store.city)}\",")
                append("\"zipCode\":\"${escape(store.zipCode)}\",")
                append("\"latitude\":${store.latitude},")
                append("\"longitude\":${store.longitude}")

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