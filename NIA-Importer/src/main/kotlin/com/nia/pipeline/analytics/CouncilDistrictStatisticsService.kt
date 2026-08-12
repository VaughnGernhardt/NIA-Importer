package com.nia.pipeline.analytics

import java.sql.Connection

class CouncilDistrictStatisticsService(
    private val connection: Connection
) {

    data class CouncilDistrictStatistic(
        val councilDistrict: String,
        val incidentCount: Int
    )

    fun generate(): List<CouncilDistrictStatistic> {

        val sql = """
            SELECT
                council_district,
                COUNT(*) AS incident_count
            FROM crime_incidents
            WHERE council_district IS NOT NULL
              AND TRIM(council_district) <> ''
            GROUP BY council_district
            ORDER BY incident_count DESC
        """.trimIndent()

        val results = mutableListOf<CouncilDistrictStatistic>()

        connection.createStatement().use { statement ->

            statement.executeQuery(sql).use { resultSet ->

                while (resultSet.next()) {

                    results.add(
                        CouncilDistrictStatistic(
                            councilDistrict = resultSet.getString("council_district"),
                            incidentCount = resultSet.getInt("incident_count")
                        )
                    )
                }
            }
        }

        return results
    }
}