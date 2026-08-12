package com.nia.pipeline.pipeline

import com.nia.pipeline.analytics.NeighborhoodStatisticsService
import com.nia.pipeline.database.DatabaseService
import com.nia.pipeline.export.geographic.NeighborhoodStatisticsExporter

class NeighborhoodStatisticsModule(
    private val databaseService: DatabaseService
) : AnalyticsModule {

    override fun execute() {

        val statistics = NeighborhoodStatisticsService(
            databaseService.getConnection()
        ).generate()

        NeighborhoodStatisticsExporter().export(statistics)
    }
}