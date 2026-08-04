package com.nia.pipeline.database

import com.nia.pipeline.logging.Log
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

    }

    private fun createTables() {

        val sql = """
            CREATE TABLE IF NOT EXISTS crime_incidents (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                incident_number TEXT,
                offense TEXT,
                offense_description TEXT,
                date_occurred TEXT,
                year INTEGER,
                latitude REAL,
                longitude REAL,
                address TEXT,
                precinct TEXT
            );
        """.trimIndent()

        connection!!.createStatement().use { statement ->
            statement.execute(sql)
        }

        Log.logger.info {
            "Verified table: crime_incidents"
        }

    }

    fun shutdown() {

        connection?.close()

        Log.logger.info {
            "Database connection closed."
        }

    }

}