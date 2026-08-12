package com.nia.pipeline.analytics

import java.sql.Connection

class YearStatisticsService(
    private val connection: Connection
) {

    data class YearStatistic(
        val year: Int,
        val incidentCount: Int
    )

    fun generate(): List<YearStatistic> {

        val sql = """
            SELECT
                year,
                COUNT(*) AS incidentCount
            FROM crime_incidents
            WHERE year IS NOT NULL
            GROUP BY year
            ORDER BY year
        """.trimIndent()

        val results = mutableListOf<YearStatistic>()

        connection.createStatement().use { statement ->

            statement.executeQuery(sql).use { resultSet ->

                while (resultSet.next()) {

                    results.add(
                        YearStatistic(
                            year = resultSet.getInt("year"),
                            incidentCount = resultSet.getInt("incidentCount")
                        )
                    )
                }
            }
        }

        return results
    }
}