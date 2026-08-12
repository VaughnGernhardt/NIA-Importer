package com.nia.pipeline.analytics

import java.sql.Connection

class NeighborhoodStatisticsService(
    private val connection: Connection
) {

    data class NeighborhoodStatistic(
        val neighborhood: String,
        val incidentCount: Int
    )

    fun generate(): List<NeighborhoodStatistic> {

        val sql = """
            SELECT
                address,
                COUNT(*) AS incidentCount
            FROM crime_incidents
            WHERE address IS NOT NULL
              AND address <> ''
            GROUP BY address
            ORDER BY incidentCount DESC
        """.trimIndent()

        val results = mutableListOf<NeighborhoodStatistic>()

        connection.createStatement().use { statement ->

            val rs = statement.executeQuery(sql)

            while (rs.next()) {

                results.add(

                    NeighborhoodStatistic(
                        neighborhood = rs.getString("address"),
                        incidentCount = rs.getInt("incidentCount")
                    )

                )
            }
        }

        return results
    }
}