package com.nia.pipeline.analytics

import com.nia.pipeline.analytics.model.DatabaseStatistics
import java.sql.Connection

class DatabaseStatisticsService(
    private val connection: Connection
) {

    fun generate(): DatabaseStatistics {

        fun queryInt(sql: String): Int {

            connection.createStatement().use { statement ->

                statement.executeQuery(sql).use { resultSet ->

                    resultSet.next()

                    return resultSet.getInt(1)
                }
            }
        }

        return DatabaseStatistics(

            totalIncidents = queryInt(
                "SELECT COUNT(*) FROM crime_incidents"
            ),

            uniqueOffenses = queryInt(
                """
                SELECT COUNT(DISTINCT offense)
                FROM crime_incidents
                WHERE offense IS NOT NULL
                """
            ),

            uniqueNeighborhoods = queryInt(
                """
                SELECT COUNT(DISTINCT address)
                FROM crime_incidents
                WHERE address IS NOT NULL
                """
            ),

            uniqueZipCodes = queryInt(
                """
                SELECT COUNT(DISTINCT zip_code)
                FROM crime_incidents
                WHERE zip_code IS NOT NULL
                """
            ),

            uniquePrecincts = queryInt(
                """
                SELECT COUNT(DISTINCT precinct)
                FROM crime_incidents
                WHERE precinct IS NOT NULL
                """
            ),

            firstCrimeYear = queryInt(
                "SELECT MIN(year) FROM crime_incidents"
            ),

            lastCrimeYear = queryInt(
                "SELECT MAX(year) FROM crime_incidents"
            )
        )
    }
}