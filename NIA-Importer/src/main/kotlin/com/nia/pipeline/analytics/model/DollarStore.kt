package com.nia.pipeline.analytics.model

data class DollarStore(

    val storeId: String,

    val storeName: String,

    val chain: String,

    val address: String,

    val city: String,

    val zipCode: String,

    val latitude: Double,

    val longitude: Double

)