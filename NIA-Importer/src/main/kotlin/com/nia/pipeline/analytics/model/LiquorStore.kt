package com.nia.pipeline.analytics.model

data class LiquorStore(

    val businessId: String,

    val businessName: String,

    val address: String,

    val city: String,

    val zipCode: String,

    val latitude: Double,

    val longitude: Double,

    val licenseType: String

)