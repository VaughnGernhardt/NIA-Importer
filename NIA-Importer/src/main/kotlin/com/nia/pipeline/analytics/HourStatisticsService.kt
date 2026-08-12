package com.nia.pipeline.analytics

import java.sql.Connection

class HourStatisticsService(
    private val connection: Connection
) {

    data class HourStatistic(
        val hour: Int,
        val incidentCount: Int
    )

    fun generate(): List<HourStatistic> {

        val sql = """
            SELECT
                CAST(strftime('%H', date_occurred) AS INTEGER) AS hour,
                COUNT(*) AS incidentCount
            FROM crime_incidents
            WHERE date_occurred IS NOT NULL
            GROUP BY hour
            ORDER BY hour
        """.trimIndent()

        val results = mutableListOf<HourStatistic>()

        connection.createStatement().use { statement ->

            val rs = statement.executeQuery(sql)

            while (rs.next()) {

                results.add(
                    HourStatistic(
                        hour = rs.getInt("hour"),
                        incidentCount = rs.getInt("incidentCount")
                    )
                )
            }
        }

        return results
    }
}