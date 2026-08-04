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

    }

    fun shutdown() {

        connection?.close()

        Log.logger.info {
            "Database connection closed."
        }

    }

}