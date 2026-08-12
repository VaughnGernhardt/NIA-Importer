package com.nia.pipeline.analytics

import com.nia.pipeline.logging.Log
import java.sql.Connection

class StatisticsService(
    private val connection: Connection
) {

    fun generateStatistics(): CrimeStatistics {

        val totalIncidents = queryInt(
            "SELECT COUNT(*) FROM crime_incidents"
        )

        val uniqueOffenses = queryInt(
            "SELECT COUNT(DISTINCT offense) FROM crime_incidents"
        )

        val uniqueNeighborhoods = queryInt(
            "SELECT COUNT(DISTINCT address) FROM crime_incidents"
        )

        val uniqueZipCodes = queryInt(
            "SELECT COUNT(DISTINCT substr(address,1,5)) FROM crime_incidents"
        )

        val firstYear = queryInt(
            "SELECT MIN(year) FROM crime_incidents"
        )

        val lastYear = queryInt(
            "SELECT MAX(year) FROM crime_incidents"
        )

        val statistics = CrimeStatistics(
            totalIncidents = totalIncidents,
            uniqueOffenses = uniqueOffenses,
            uniqueNeighborhoods = uniqueNeighborhoods,
            uniqueZipCodes = uniqueZipCodes,
            firstYear = firstYear,
            lastYear = lastYear
        )

        Log.logger.info { "========== DATABASE STATISTICS ==========" }
        Log.logger.info { "Total Incidents      : ${statistics.totalIncidents}" }
        Log.logger.info { "Unique Offenses      : ${statistics.uniqueOffenses}" }
        Log.logger.info { "Unique Locations     : ${statistics.uniqueNeighborhoods}" }
        Log.logger.info { "Unique ZIP Codes     : ${statistics.uniqueZipCodes}" }
        Log.logger.info { "First Crime Year     : ${statistics.firstYear}" }
        Log.logger.info { "Last Crime Year      : ${statistics.lastYear}" }
        Log.logger.info { "=========================================" }

        return statistics
    }

    private fun queryInt(sql: String): Int {

        connection.createStatement().use { statement ->

            statement.executeQuery(sql).use { result ->

                result.next()

                return result.getInt(1)

            }

        }

    }

}