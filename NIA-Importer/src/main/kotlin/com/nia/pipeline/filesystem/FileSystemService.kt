package com.nia.pipeline.filesystem

import com.nia.pipeline.logging.Log
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class FileSystemService {

    fun initialize() {

        createDirectory("data")
        createDirectory("data/input")
        createDirectory("data/output")
        createDirectory("data/logs")
        createDirectory("data/temp")

    }

    private fun createDirectory(directory: String) {

        val path: Path = Paths.get(directory)

        if (Files.exists(path)) {

            Log.logger.info { "Directory exists: $directory" }

        } else {

            Files.createDirectories(path)

            Log.logger.info { "Created directory: $directory" }

        }

    }

}