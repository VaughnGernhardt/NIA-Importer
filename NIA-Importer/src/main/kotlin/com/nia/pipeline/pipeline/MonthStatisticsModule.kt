package com.nia.pipeline.pipeline

import com.nia.pipeline.analytics.MonthStatisticsService
import com.nia.pipeline.database.DatabaseService
import com.nia.pipeline.export.time.MonthStatisticsExporter

class MonthStatisticsModule(
    private val databaseService: DatabaseService
) : AnalyticsModule {

    override fun execute() {

        val statistics = MonthStatisticsService(
            databaseService.getConnection()
        ).generate()

        MonthStatisticsExporter().export(statistics)
    }
}