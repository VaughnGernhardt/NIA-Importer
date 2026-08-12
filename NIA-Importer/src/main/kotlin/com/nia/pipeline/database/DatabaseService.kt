package com.nia.pipeline.database

import com.nia.pipeline.logging.Log
import com.nia.pipeline.model.CrimeIncident
import java.sql.Connection
import java.sql.DriverManager

class DatabaseService {

    private var connection: Connection? = null

    fun initialize() {

        val databasePath = "data/output/crime.db"

        connection = DriverManager.getConnection(
            "jdbc:sqlite:$databasePath"
        )

        Log.logger.info {
            "Connected to SQLite database: $databasePath"
        }

        createTables()
        createIndexes()
    }

    private fun createTables() {

        val sql = """
            CREATE TABLE IF NOT EXISTS crime_incidents (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                incident_number TEXT NOT NULL UNIQUE,
                offense TEXT,
                offense_description TEXT,
                date_occurred TEXT,
                year INTEGER,
                latitude REAL,
                longitude REAL,
                address TEXT,
                precinct TEXT,
                zip_code TEXT,
                neighborhood TEXT,
                council_district TEXT,
                hour_of_day INTEGER,
                day_of_week INTEGER
            );
        """.trimIndent()

        connection!!.createStatement().use {
            it.execute(sql)
        }

        Log.logger.info {
            "Verified table: crime_incidents"
        }
    }

    private fun createIndexes() {

        val indexes = listOf(

            """
            CREATE INDEX IF NOT EXISTS idx_crime_year
            ON crime_incidents(year);
            """.trimIndent(),

            """
            CREATE INDEX IF NOT EXISTS idx_crime_offense
            ON crime_incidents(offense);
            """.trimIndent(),

            """
            CREATE INDEX IF NOT EXISTS idx_crime_latitude
            ON crime_incidents(latitude);
            """.trimIndent(),

            """
            CREATE INDEX IF NOT EXISTS idx_crime_longitude
            ON crime_incidents(longitude);
            """.trimIndent(),

            """
            CREATE INDEX IF NOT EXISTS idx_crime_zip
            ON crime_incidents(zip_code);
            """.trimIndent(),

            """
            CREATE INDEX IF NOT EXISTS idx_crime_neighborhood
            ON crime_incidents(neighborhood);
            """.trimIndent(),

            """
            CREATE INDEX IF NOT EXISTS idx_crime_hour
            ON crime_incidents(hour_of_day);
            """.trimIndent(),

            """
            CREATE INDEX IF NOT EXISTS idx_crime_day
            ON crime_incidents(day_of_week);
            """.trimIndent()

        )

        connection!!.createStatement().use { statement ->
            indexes.forEach(statement::execute)
        }

        Log.logger.info {
            "Verified database indexes."
        }
    }

    fun clearCrimeTable() {

        connection!!.createStatement().use {

            it.execute("DELETE FROM crime_incidents")
            it.execute("DELETE FROM sqlite_sequence WHERE name='crime_incidents'")

        }

        Log.logger.info {
            "Cleared existing crime data."
        }
    }

    fun importCrimeIncidents(crimes: Sequence<CrimeIncident>) {

        clearCrimeTable()

        val sql = """
            INSERT OR IGNORE INTO crime_incidents (
                incident_number,
                offense,
                offense_description,
                date_occurred,
                year,
                latitude,
                longitude,
                address,
                precinct,
                zip_code,
                neighborhood,
                council_district,
                hour_of_day,
                day_of_week
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()

        connection!!.autoCommit = false

        connection!!.prepareStatement(sql).use { statement ->

            var count = 0

            crimes.forEach { crime ->

                statement.setString(1, crime.incidentNumber)
                statement.setString(2, crime.offense)
                statement.setString(3, crime.offenseDescription)
                statement.setString(4, crime.dateOccurred)
                statement.setInt(5, crime.year)
                statement.setDouble(6, crime.latitude)
                statement.setDouble(7, crime.longitude)
                statement.setString(8, crime.address)
                statement.setString(9, crime.precinct)
                statement.setString(10, crime.zipCode)
                statement.setString(11, crime.neighborhood)
                statement.setString(12, crime.councilDistrict)
                statement.setInt(13, crime.hourOfDay)
                statement.setInt(14, crime.dayOfWeek)

                statement.addBatch()

                count++

                if (count % 1000 == 0) {
                    statement.executeBatch()
                    Log.logger.info { "Imported $count records..." }
                }
            }

            statement.executeBatch()
        }

        connection!!.commit()
        connection!!.autoCommit = true

        Log.logger.info {
            "Bulk import complete."
        }
    }

    fun getConnection(): Connection {

        return connection
            ?: throw IllegalStateException("Database has not been initialized.")

    }

    fun shutdown() {

        connection?.close()

        Log.logger.info {
            "Database connection closed."
        }
    }
}