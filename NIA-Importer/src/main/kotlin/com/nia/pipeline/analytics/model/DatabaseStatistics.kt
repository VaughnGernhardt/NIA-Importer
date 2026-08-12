package com.nia.pipeline.analytics.model

data class DatabaseStatistics(

    val totalIncidents: Int,

    val uniqueOffenses: Int,

    val uniqueNeighborhoods: Int,

    val uniqueZipCodes: Int,

    val uniquePrecincts: Int,

    val firstCrimeYear: Int,

    val lastCrimeYear: Int

)