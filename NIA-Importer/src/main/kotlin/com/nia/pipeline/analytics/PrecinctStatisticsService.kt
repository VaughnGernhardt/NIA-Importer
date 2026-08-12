package com.nia.pipeline.analytics

import java.sql.Connection

class PrecinctStatisticsService(
    private val connection: Connection
) {

    data class PrecinctStatistic(
        val precinct: String,
        val incidentCount: Int
    )

    fun generate(): List<PrecinctStatistic> {

        val sql = """
            SELECT
                precinct,
                COUNT(*) AS incident_count
            FROM crime_incidents
            WHERE precinct IS NOT NULL
              AND TRIM(precinct) <> ''
            GROUP BY precinct
            ORDER BY incident_count DESC
        """.trimIndent()

        val results = mutableListOf<PrecinctStatistic>()

        connection.createStatement().use { statement ->

            statement.executeQuery(sql).use { resultSet ->

                while (resultSet.next()) {

                    results.add(
                        PrecinctStatistic(
                            precinct = resultSet.getString("precinct"),
                            incidentCount = resultSet.getInt("incident_count")
                        )
                    )
                }
            }
        }

        return results
    }
}