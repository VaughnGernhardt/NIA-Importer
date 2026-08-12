package com.nia.pipeline.analytics.model

data class MedianIncomeArea(

    val zipCode: String,

    val medianHouseholdIncome: Int,

    val latitude: Double,

    val longitude: Double

)