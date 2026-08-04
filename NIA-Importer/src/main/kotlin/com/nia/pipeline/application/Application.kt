package com.nia.pipeline.application

import com.nia.pipeline.config.AppConfig
import com.nia.pipeline.logging.Log
import com.nia.pipeline.service.ServiceRegistry

class Application {

    private val services = ServiceRegistry()

    fun start() {

        Log.logger.info { "=========================================" }
        Log.logger.info { AppConfig.APPLICATION_NAME }
        Log.logger.info { "Version ${AppConfig.APPLICATION_VERSION}" }
        Log.logger.info { "=========================================" }

        services.fileSystemService.initialize()

        services.databaseService.initialize()

        Log.logger.info {
            "Application started successfully."
        }

    }

}