package com.nia.pipeline.pipeline

import com.nia.pipeline.analytics.CouncilDistrictStatisticsService
import com.nia.pipeline.database.DatabaseService
import com.nia.pipeline.export.geographic.CouncilDistrictStatisticsExporter

class CouncilDistrictStatisticsModule(
    private val databaseService: DatabaseService
) : AnalyticsModule {

    override fun execute() {

        val statistics = CouncilDistrictStatisticsService(
            databaseService.getConnection()
        ).generate()

        CouncilDistrictStatisticsExporter().export(statistics)
    }
}