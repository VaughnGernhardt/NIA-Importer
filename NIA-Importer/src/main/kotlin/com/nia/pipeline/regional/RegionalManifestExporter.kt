package com.nia.pipeline.regional

import com.google.gson.GsonBuilder
import com.nia.pipeline.logging.Log
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class RegionalManifestExporter {

    private val gson =
        GsonBuilder()
            .setPrettyPrinting()
            .create()

    fun export(
        entries: List<RegionalTileManifestEntry>
    ) {

        val outputPath =
            Paths.get(
                "data",
                "output",
                "regional",
                "regional_manifest.json"
            )

        ensureParentDirectory(
            outputPath
        )

        val json =
            gson.toJson(
                entries
            )

        Files.writeString(
            outputPath,
            json
        )

        Log.logger.info {
            "Regional manifest exported: $outputPath"
        }

        Log.logger.info {
            "Regional manifest entries: ${entries.size}"
        }
    }

    private fun ensureParentDirectory(
        outputPath: Path
    ) {

        val parent =
            outputPath.parent
                ?: return

        if (
            !Files.exists(
                parent
            )
        ) {

            Files.createDirectories(
                parent
            )
        }
    }
}