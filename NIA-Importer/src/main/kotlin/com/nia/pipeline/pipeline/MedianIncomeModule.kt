package com.nia.pipeline.pipeline

import com.nia.pipeline.analytics.MedianIncomeStatisticsService
import com.nia.pipeline.database.DatabaseService
import com.nia.pipeline.export.income.MedianIncomeExporter
import com.nia.pipeline.logging.Log

class MedianIncomeModule(
    private val databaseService: DatabaseService
) : AnalyticsModule {

    override fun execute() {

        Log.logger.info {
            "Generating median income layer..."
        }

        val connection =
            databaseService.getConnection()

        val statisticsService =
            MedianIncomeStatisticsService(
                connection
            )

        val exporter =
            MedianIncomeExporter()

        val areas =
            statisticsService.generate()

        exporter.export(
            areas = areas
        )

        Log.logger.info {
            "Median income layer complete."
        }
    }
}