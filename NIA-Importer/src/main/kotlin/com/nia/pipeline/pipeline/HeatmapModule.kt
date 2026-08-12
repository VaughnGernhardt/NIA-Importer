package com.nia.pipeline.pipeline

import com.nia.pipeline.analytics.HeatmapGridService
import com.nia.pipeline.database.DatabaseService
import com.nia.pipeline.export.heatmap.HeatmapGridExporter
import com.nia.pipeline.logging.Log

class HeatmapModule(
    private val databaseService: DatabaseService
) : AnalyticsModule {

    override fun execute() {

        val connection =
            databaseService.getConnection()

        val heatmapService =
            HeatmapGridService(connection)

        val exporter =
            HeatmapGridExporter()

        exporter.export(
            cells = heatmapService.generate(),
            fileName = "crime_heatmap_grid.json"
        )

        heatmapService.availableYears().forEach { year ->

            Log.logger.info {
                "Generating heatmap for year $year..."
            }

            exporter.export(
                cells = heatmapService.generate(
                    year = year
                ),
                fileName = "crime_heatmap_grid_$year.json"
            )
        }

        heatmapService.availableZipCodes().forEach { zipCode ->

            Log.logger.info {
                "Generating heatmap for ZIP $zipCode..."
            }

            exporter.export(
                cells = heatmapService.generate(
                    zipCode = zipCode
                ),
                fileName = "crime_heatmap_grid_zip_$zipCode.json"
            )
        }

        Log.logger.info {
            "Heatmap generation complete."
        }
    }
}