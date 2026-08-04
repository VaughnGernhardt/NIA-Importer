package com.nia.pipeline.service

import com.nia.pipeline.database.DatabaseService
import com.nia.pipeline.filesystem.FileSystemService

class ServiceRegistry {

    val fileSystemService = FileSystemService()

    val databaseService = DatabaseService()

}