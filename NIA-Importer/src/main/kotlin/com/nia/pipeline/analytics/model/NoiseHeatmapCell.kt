package com.nia.pipeline.analytics.model

data class NoiseHeatmapCell(
    val latitude: Double,
    val longitude: Double,
    val decibels: Double,
    val measurementCount: Int
)