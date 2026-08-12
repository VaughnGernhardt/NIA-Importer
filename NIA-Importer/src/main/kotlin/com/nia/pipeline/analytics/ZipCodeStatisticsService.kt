package com.nia.pipeline.analytics

import java.sql.Connection

class ZipCodeStatisticsService(
    private val connection: Connection
) {

    data class ZipCodeStatistic(
        val zipCode: String,
        val incidentCount: Int
    )

    fun generate(): List<ZipCodeStatistic> {

        val sql = """
            SELECT
                zip_code,
                COUNT(*) AS incidentCount
            FROM crime_incidents
            WHERE zip_code IS NOT NULL
              AND zip_code <> ''
            GROUP BY zip_code
            ORDER BY incidentCount DESC
        """.trimIndent()

        val results = mutableListOf<ZipCodeStatistic>()

        connection.createStatement().use { statement ->

            val rs = statement.executeQuery(sql)

            while (rs.next()) {

                results.add(
                    ZipCodeStatistic(
                        zipCode = rs.getString("zip_code"),
                        incidentCount = rs.getInt("incidentCount")
                    )
                )
            }
        }

        return results
    }
}