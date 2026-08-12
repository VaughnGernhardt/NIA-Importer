package com.nia.pipeline.analytics

import java.sql.Connection

class MonthStatisticsService(
    private val connection: Connection
) {

    data class MonthStatistic(
        val month: Int,
        val incidentCount: Int
    )

    fun generate(): List<MonthStatistic> {

        val sql = """
            SELECT
                CAST(SUBSTR(date_occurred, 6, 2) AS INTEGER) AS month,
                COUNT(*) AS incidentCount
            FROM crime_incidents
            WHERE date_occurred IS NOT NULL
              AND date_occurred <> ''
            GROUP BY month
            ORDER BY month
        """.trimIndent()

        val results = mutableListOf<MonthStatistic>()

        connection.createStatement().use { statement ->
            statement.executeQuery(sql).use { resultSet ->

                while (resultSet.next()) {
                    results.add(
                        MonthStatistic(
                            month = resultSet.getInt("month"),
                            incidentCount = resultSet.getInt("incidentCount")
                        )
                    )
                }
            }
        }

        return results
    }
}