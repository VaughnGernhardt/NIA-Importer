package com.nia.pipeline.pipeline

import com.nia.pipeline.analytics.NoiseHeatmapService
import com.nia.pipeline.analytics.NoiseStatisticsService
import com.nia.pipeline.export.noise.NoiseHeatmapExporter
import com.nia.pipeline.logging.Log

class NoiseModule : AnalyticsModule {

    override fun execute() {

        Log.logger.info {
            "Generating noise layer..."
        }

        val statisticsService =
            NoiseStatisticsService()

        val heatmapService =
            NoiseHeatmapService()

        val exporter =
            NoiseHeatmapExporter()

        val points =
            statisticsService.generate()

        val cells =
            heatmapService.generate(
                points = points
            )

        exporter.export(
            cells = cells
        )

        Log.logger.info {
            "Noise layer complete."
        }
    }
}