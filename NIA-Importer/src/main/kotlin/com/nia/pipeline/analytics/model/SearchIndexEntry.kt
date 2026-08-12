package com.nia.pipeline.analytics.model

data class SearchIndexEntry(

    val incidentNumber: String,

    val offense: String,

    val neighborhood: String,

    val zipCode: String,

    val precinct: String,

    val councilDistrict: String,

    val latitude: Double,

    val longitude: Double

)