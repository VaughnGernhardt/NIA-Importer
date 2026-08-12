package com.nia.pipeline.analytics

data class CrimeStatistics(

    val totalIncidents: Int,

    val uniqueOffenses: Int,

    val uniqueNeighborhoods: Int,

    val uniqueZipCodes: Int,

    val firstYear: Int,

    val lastYear: Int

)