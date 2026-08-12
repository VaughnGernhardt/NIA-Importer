package com.nia.pipeline.analytics

import java.sql.Connection

class DayOfWeekStatisticsService(
    private val connection: Connection
) {

    data class DayOfWeekStatistic(
        val dayOfWeek: String,
        val incidentCount: Int
    )

    fun generate(): List<DayOfWeekStatistic> {

        val sql = """
            SELECT
                CASE day_of_week
                    WHEN 0 THEN 'Sunday'
                    WHEN 1 THEN 'Monday'
                    WHEN 2 THEN 'Tuesday'
                    WHEN 3 THEN 'Wednesday'
                    WHEN 4 THEN 'Thursday'
                    WHEN 5 THEN 'Friday'
                    WHEN 6 THEN 'Saturday'
                    ELSE 'Unknown'
                END AS day_name,
                day_of_week,
                COUNT(*) AS incident_count
            FROM crime_incidents
            WHERE day_of_week BETWEEN 0 AND 6
            GROUP BY day_of_week
            ORDER BY day_of_week
        """.trimIndent()

        val results = mutableListOf<DayOfWeekStatistic>()

        connection.createStatement().use { statement ->

            statement.executeQuery(sql).use { resultSet ->

                while (resultSet.next()) {

                    results.add(
                        DayOfWeekStatistic(
                            dayOfWeek = resultSet.getString("day_name"),
                            incidentCount = resultSet.getInt("incident_count")
                        )
                    )
                }
            }
        }

        return results
    }
}