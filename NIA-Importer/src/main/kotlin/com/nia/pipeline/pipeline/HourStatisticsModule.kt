package com.nia.pipeline.pipeline

import com.nia.pipeline.analytics.HourStatisticsService
import com.nia.pipeline.database.DatabaseService
import com.nia.pipeline.export.time.HourStatisticsExporter

class HourStatisticsModule(
    private val databaseService: DatabaseService
) : AnalyticsModule {

    override fun execute() {

        val statistics = HourStatisticsService(
            databaseService.getConnection()
        ).generate()

        HourStatisticsExporter().export(statistics)
    }
}