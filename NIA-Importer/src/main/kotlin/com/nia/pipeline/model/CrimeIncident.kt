package com.nia.pipeline.model

data class CrimeIncident(

    val incidentNumber: String,

    val offense: String,

    val offenseDescription: String,

    val dateOccurred: String,

    val year: Int,

    val latitude: Double,

    val longitude: Double,

    val address: String,

    val precinct: String,

    val zipCode: String,

    val neighborhood: String,

    val councilDistrict: String,

    val hourOfDay: Int,

    val dayOfWeek: Int

)