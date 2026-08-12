package com.nia.pipeline.analytics

import com.nia.pipeline.analytics.model.HeatmapCell
import java.sql.Connection

class HeatmapGridService(
    private val connection: Connection
) {

    fun generate(
        year: Int? = null,
        zipCode: String? = null
    ): List<HeatmapCell> {

        val sql = buildString {

            appendLine(
                """
                SELECT
                    ROUND(latitude, 2) AS grid_latitude,
                    ROUND(longitude, 2) AS grid_longitude,
                    COUNT(*) AS intensity
                FROM crime_incidents
                WHERE latitude IS NOT NULL
                  AND longitude IS NOT NULL
                  AND latitude BETWEEN 42.0 AND 43.0
                  AND longitude BETWEEN -84.0 AND -82.0
                """.trimIndent()
            )

            if (year != null) {
                appendLine("  AND year = ?")
            }

            if (zipCode != null) {
                appendLine("  AND zip_code = ?")
            }

            appendLine(
                """
                GROUP BY
                    ROUND(latitude, 2),
                    ROUND(longitude, 2)
                ORDER BY intensity DESC
                """.trimIndent()
            )
        }

        val cells = mutableListOf<HeatmapCell>()

        connection.prepareStatement(sql).use { statement ->

            var parameterIndex = 1

            if (year != null) {
                statement.setInt(
                    parameterIndex++,
                    year
                )
            }

            if (zipCode != null) {
                statement.setString(
                    parameterIndex++,
                    zipCode
                )
            }

            statement.executeQuery().use { resultSet ->

                while (resultSet.next()) {

                    cells.add(
                        HeatmapCell(
                            latitude =
                                resultSet.getDouble(
                                    "grid_latitude"
                                ),
                            longitude =
                                resultSet.getDouble(
                                    "grid_longitude"
                                ),
                            intensity =
                                resultSet.getInt(
                                    "intensity"
                                )
                        )
                    )
                }
            }
        }

        return cells
    }

    fun availableYears(): List<Int> {

        val sql = """
            SELECT DISTINCT year
            FROM crime_incidents
            WHERE year IS NOT NULL
            ORDER BY year DESC
        """.trimIndent()

        val years = mutableListOf<Int>()

        connection.createStatement().use { statement ->

            statement.executeQuery(sql).use { resultSet ->

                while (resultSet.next()) {

                    years.add(
                        resultSet.getInt("year")
                    )
                }
            }
        }

        return years
    }

    fun availableZipCodes(): List<String> {

        val sql = """
            SELECT DISTINCT zip_code
            FROM crime_incidents
            WHERE zip_code IS NOT NULL
              AND zip_code <> ''
            ORDER BY zip_code
        """.trimIndent()

        val zipCodes = mutableListOf<String>()

        connection.createStatement().use { statement ->

            statement.executeQuery(sql).use { resultSet ->

                while (resultSet.next()) {

                    zipCodes.add(
                        resultSet.getString("zip_code")
                    )
                }
            }
        }

        return zipCodes
    }
}