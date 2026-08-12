package com.nia.pipeline.pipeline

import com.nia.pipeline.analytics.DollarStoreStatisticsService
import com.nia.pipeline.export.dollar.DollarStoreExporter
import com.nia.pipeline.logging.Log

class DollarStoreModule : AnalyticsModule {

    override fun execute() {

        Log.logger.info {
            "Generating dollar store layer..."
        }

        val statisticsService =
            DollarStoreStatisticsService()

        val exporter =
            DollarStoreExporter()

        val stores =
            statisticsService.generate()

        exporter.export(
            stores = stores
        )

        Log.logger.info {
            "Dollar store layer complete."
        }
    }
}