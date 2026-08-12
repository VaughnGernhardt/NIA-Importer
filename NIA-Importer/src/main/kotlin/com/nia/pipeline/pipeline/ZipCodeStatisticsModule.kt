package com.nia.pipeline.pipeline

import com.nia.pipeline.analytics.ZipCodeStatisticsService
import com.nia.pipeline.database.DatabaseService
import com.nia.pipeline.export.JsonExporter
import com.nia.pipeline.logging.Log
import java.io.File

class ZipCodeStatisticsModule(
    private val databaseService: DatabaseService
) : AnalyticsModule {

    override fun execute() {

        val statistics =
            ZipCodeStatisticsService(
                databaseService.getConnection()
            ).generate()

        val outputDirectory =
            File("data/output/analytics")

        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs()
        }

        val outputFile =
            File(
                outputDirectory,
                "zip_code_statistics.json"
            )

        val json = buildString {

            appendLine("[")

            statistics.forEachIndexed { index, item ->

                append("  {")
                append("\"zipCode\":\"${item.zipCode}\",")
                append("\"incidentCount\":${item.incidentCount}")
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
            "Exported ${statistics.size} ZIP code statistics to ${outputFile.absolutePath}."
        }
    }
}