package com.nia.pipeline.pipeline

import com.nia.pipeline.logging.Log

class AnalyticsPipeline {

    private val modules = mutableListOf<AnalyticsModule>()

    fun add(module: AnalyticsModule): AnalyticsPipeline {

        modules.add(module)

        return this
    }

    fun run() {

        Log.logger.info {
            "========================================="
        }

        Log.logger.info {
            "Starting analytics pipeline..."
        }

        Log.logger.info {
            "Modules: ${modules.size}"
        }

        Log.logger.info {
            "========================================="
        }

        modules.forEach {

            Log.logger.info {
                "Running ${it.javaClass.simpleName}"
            }

            it.execute()
        }

        Log.logger.info {
            "========================================="
        }

        Log.logger.info {
            "Analytics pipeline complete."
        }

        Log.logger.info {
            "========================================="
        }
    }
}