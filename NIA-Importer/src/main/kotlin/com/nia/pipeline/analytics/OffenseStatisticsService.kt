package com.nia.pipeline.analytics

import com.nia.pipeline.analytics.model.OffenseStatistic
import com.nia.pipeline.logging.Log
import java.sql.Connection

class OffenseStatisticsService(
    private val connection: Connection
) {

    fun generate(): List<OffenseStatistic> {

        val results = mutableListOf<OffenseStatistic>()

        val sql = """
            SELECT
                offense,
                COUNT(*) AS incident_count
            FROM crime_incidents
            WHERE offense IS NOT NULL
              AND TRIM(offense) <> ''
            GROUP BY offense
            ORDER BY incident_count DESC
        """.trimIndent()

        connection.createStatement().use { statement ->

            statement.executeQuery(sql).use { resultSet ->

                while (resultSet.next()) {

                    results.add(
                        OffenseStatistic(
                            offense = resultSet.getString("offense"),
                            incidentCount = resultSet.getInt("incident_count")
                        )
                    )
                }
            }
        }

        Log.logger.info {
            "Generated ${results.size} offense statistics."
        }

        return results
    }
}