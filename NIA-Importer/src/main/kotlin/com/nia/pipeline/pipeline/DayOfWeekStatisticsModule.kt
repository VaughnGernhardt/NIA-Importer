package com.nia.pipeline.pipeline

import com.nia.pipeline.analytics.DayOfWeekStatisticsService
import com.nia.pipeline.database.DatabaseService
import com.nia.pipeline.export.time.DayOfWeekStatisticsExporter

class DayOfWeekStatisticsModule(
    private val databaseService: DatabaseService
) : AnalyticsModule {

    override fun execute() {

        val statistics = DayOfWeekStatisticsService(
            databaseService.getConnection()
        ).generate()

        DayOfWeekStatisticsExporter().export(statistics)
    }
}