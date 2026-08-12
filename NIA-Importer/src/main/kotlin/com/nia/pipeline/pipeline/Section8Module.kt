package com.nia.pipeline.pipeline

import com.nia.pipeline.analytics.Section8StatisticsService
import com.nia.pipeline.export.section8.Section8Exporter
import com.nia.pipeline.logging.Log

class Section8Module : AnalyticsModule {

    override fun execute() {

        Log.logger.info {
            "Generating Section 8 layer..."
        }

        val statisticsService =
            Section8StatisticsService()

        val exporter =
            Section8Exporter()

        val properties =
            statisticsService.generate()

        exporter.export(
            properties = properties
        )

        Log.logger.info {
            "Section 8 layer complete."
        }
    }
}