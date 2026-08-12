package com.nia.pipeline.regional

data class RegionalTileManifestEntry(

    val id: String,

    val state: String,

    val county: String? = null,

    val south: Double,

    val west: Double,

    val north: Double,

    val east: Double,

    val crimeFile: String? = null,

    val vacantPropertyFile: String? = null,

    val medianIncomeFile: String? = null,

    val section8File: String? = null,

    val liquorStoreFile: String? = null,

    val dollarStoreFile: String? = null,

    val noiseFile: String? = null
)