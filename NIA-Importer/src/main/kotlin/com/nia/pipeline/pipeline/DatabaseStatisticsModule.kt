package com.nia.pipeline.pipeline

import com.nia.pipeline.analytics.DatabaseStatisticsService
import com.nia.pipeline.database.DatabaseService
import com.nia.pipeline.export.metadata.DatabaseStatisticsExporter

class DatabaseStatisticsModule(
    private val databaseService: DatabaseService
) : AnalyticsModule {

    override fun execute() {

        val statistics = DatabaseStatisticsService(
            databaseService.getConnection()
        ).generate()

        DatabaseStatisticsExporter().export(statistics)
    }
}