package com.nia.pipeline.analytics

import com.nia.pipeline.analytics.model.NoiseHeatmapCell
import com.nia.pipeline.analytics.model.NoisePoint

class NoiseHeatmapService {

    fun generate(
        points: List<NoisePoint>
    ): List<NoiseHeatmapCell> {

        if (points.isEmpty()) {
            return emptyList()
        }

        return points
            .groupBy { point ->

                Pair(
                    roundCoordinate(point.latitude),
                    roundCoordinate(point.longitude)
                )
            }
            .map { (location, measurements) ->

                NoiseHeatmapCell(
                    latitude = location.first,
                    longitude = location.second,
                    decibels = measurements
                        .map { it.decibels }
                        .average(),
                    measurementCount = measurements.size
                )
            }
            .sortedByDescending {
                it.decibels
            }
    }

    private fun roundCoordinate(
        value: Double
    ): Double {

        return kotlin.math.round(
            value * 100.0
        ) / 100.0
    }
}