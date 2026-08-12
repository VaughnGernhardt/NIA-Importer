package com.nia.pipeline.export

import java.io.File

abstract class JsonExporter {

    protected fun ensureOutputDirectory(
        directory: String
    ): File {

        val outputDirectory = File(directory)

        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs()
        }

        return outputDirectory
    }

    protected fun escape(value: String): String {

        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
    }
}