package com.nia.pipeline.analytics

import com.nia.pipeline.analytics.model.SearchIndexEntry
import java.sql.Connection

class SearchIndexService(
    private val connection: Connection
) {

    fun generate(): Sequence<SearchIndexEntry> {

        val sql = """
            SELECT
                incident_number,
                offense,
                address,
                precinct,
                latitude,
                longitude
            FROM crime_incidents
            WHERE latitude IS NOT NULL
              AND longitude IS NOT NULL
        """.trimIndent()

        return sequence {

            connection.createStatement().use { statement ->

                statement.fetchSize = 5000

                statement.executeQuery(sql).use { resultSet ->

                    while (resultSet.next()) {

                        yield(

                            SearchIndexEntry(

                                incidentNumber =
                                    resultSet.getString("incident_number"),

                                offense =
                                    resultSet.getString("offense") ?: "",

                                neighborhood =
                                    resultSet.getString("address") ?: "",

                                zipCode = "",

                                precinct =
                                    resultSet.getString("precinct") ?: "",

                                councilDistrict = "",

                                latitude =
                                    resultSet.getDouble("latitude"),

                                longitude =
                                    resultSet.getDouble("longitude")
                            )
                        )
                    }
                }
            }
        }
    }
}