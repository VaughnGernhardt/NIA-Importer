package com.nia.pipeline.analytics.model

data class NoisePoint(
    val latitude: Double,
    val longitude: Double,
    val decibels: Double,
    val timestamp: String
)