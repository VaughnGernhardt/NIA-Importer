package com.nia.pipeline.analytics.model

data class FilteredHeatmapCell(
    val latitude: Double,
    val longitude: Double,
    val intensity: Int,
    val year: Int,
    val offense: String
)