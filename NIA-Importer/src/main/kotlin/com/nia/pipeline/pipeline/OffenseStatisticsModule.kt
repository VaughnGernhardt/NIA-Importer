package com.nia.pipeline.pipeline

import com.nia.pipeline.analytics.OffenseStatisticsService
import com.nia.pipeline.database.DatabaseService
import com.nia.pipeline.export.offense.OffenseStatisticsExporter

class OffenseStatisticsModule(
    private val databaseService: DatabaseService
) : AnalyticsModule {

    override fun execute() {

        val statistics = OffenseStatisticsService(
            databaseService.getConnection()
        ).generate()

        OffenseStatisticsExporter().export(statistics)
    }
}