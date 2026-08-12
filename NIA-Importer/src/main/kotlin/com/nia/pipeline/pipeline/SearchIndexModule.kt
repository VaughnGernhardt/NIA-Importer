package com.nia.pipeline.pipeline

import com.nia.pipeline.analytics.SearchIndexService
import com.nia.pipeline.database.DatabaseService
import com.nia.pipeline.export.SearchIndexExporter

class SearchIndexModule(
    private val databaseService: DatabaseService
) : AnalyticsModule {

    override fun execute() {

        val entries = SearchIndexService(
            databaseService.getConnection()
        ).generate()

        SearchIndexExporter().export(entries)
    }
}