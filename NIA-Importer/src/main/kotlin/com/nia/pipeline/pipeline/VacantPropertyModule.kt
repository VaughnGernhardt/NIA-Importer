package com.nia.pipeline.pipeline

import com.nia.pipeline.analytics.VacantPropertyStatisticsService
import com.nia.pipeline.export.vacancy.VacantPropertyExporter
import com.nia.pipeline.logging.Log

class VacantPropertyModule : AnalyticsModule {

    override fun execute() {

        Log.logger.info {
            "Generating vacant property layer..."
        }

        val statisticsService =
            VacantPropertyStatisticsService()

        val exporter =
            VacantPropertyExporter()

        val properties =
            statisticsService.generate()

        exporter.export(
            properties = properties
        )

        Log.logger.info {
            "Vacant property layer complete."
        }
    }
}