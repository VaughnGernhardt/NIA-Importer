package com.nia.pipeline.pipeline

import com.nia.pipeline.analytics.LiquorStoreStatisticsService
import com.nia.pipeline.export.liquor.LiquorStoreExporter
import com.nia.pipeline.logging.Log

class LiquorStoreModule : AnalyticsModule {

    override fun execute() {

        Log.logger.info {
            "Generating liquor store layer..."
        }

        val statisticsService =
            LiquorStoreStatisticsService()

        val exporter =
            LiquorStoreExporter()

        val stores =
            statisticsService.generate()

        exporter.export(
            stores = stores
        )

        Log.logger.info {
            "Liquor store layer complete."
        }
    }
}