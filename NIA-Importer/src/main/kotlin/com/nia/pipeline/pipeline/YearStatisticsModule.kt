package com.nia.pipeline.pipeline

import com.nia.pipeline.analytics.YearStatisticsService
import com.nia.pipeline.database.DatabaseService
import com.nia.pipeline.export.time.YearStatisticsExporter

class YearStatisticsModule(
    private val databaseService: DatabaseService
) : AnalyticsModule {

    override fun execute() {

        val statistics = YearStatisticsService(
            databaseService.getConnection()
        ).generate()

        YearStatisticsExporter().export(statistics)
    }
}