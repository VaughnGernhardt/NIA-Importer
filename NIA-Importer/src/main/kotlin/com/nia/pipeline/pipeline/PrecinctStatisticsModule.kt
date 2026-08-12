package com.nia.pipeline.pipeline

import com.nia.pipeline.analytics.PrecinctStatisticsService
import com.nia.pipeline.database.DatabaseService
import com.nia.pipeline.export.geographic.PrecinctStatisticsExporter

class PrecinctStatisticsModule(
    private val databaseService: DatabaseService
) : AnalyticsModule {

    override fun execute() {

        val statistics = PrecinctStatisticsService(
            databaseService.getConnection()
        ).generate()

        PrecinctStatisticsExporter().export(statistics)
    }
}