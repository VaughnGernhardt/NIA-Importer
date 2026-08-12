package com.nia.pipeline.application

import com.nia.pipeline.config.AppConfig
import com.nia.pipeline.csv.CsvImportService
import com.nia.pipeline.logging.Log
import com.nia.pipeline.pipeline.AnalyticsPipeline
import com.nia.pipeline.pipeline.CouncilDistrictStatisticsModule
import com.nia.pipeline.pipeline.DatabaseStatisticsModule
import com.nia.pipeline.pipeline.DayOfWeekStatisticsModule
import com.nia.pipeline.pipeline.DollarStoreModule
import com.nia.pipeline.pipeline.HeatmapModule
import com.nia.pipeline.pipeline.HourStatisticsModule
import com.nia.pipeline.pipeline.LiquorStoreModule
import com.nia.pipeline.pipeline.MedianIncomeModule
import com.nia.pipeline.pipeline.MonthStatisticsModule
import com.nia.pipeline.pipeline.NeighborhoodStatisticsModule
import com.nia.pipeline.pipeline.NoiseModule
import com.nia.pipeline.pipeline.OffenseStatisticsModule
import com.nia.pipeline.pipeline.PrecinctStatisticsModule
import com.nia.pipeline.pipeline.RegionalTileModule
import com.nia.pipeline.pipeline.SearchIndexModule
import com.nia.pipeline.pipeline.Section8Module
import com.nia.pipeline.pipeline.VacantPropertyModule
import com.nia.pipeline.pipeline.YearStatisticsModule
import com.nia.pipeline.pipeline.ZipCodeStatisticsModule
import com.nia.pipeline.service.ServiceRegistry

class Application {

    private val services =
        ServiceRegistry()

    fun start() {

        Log.logger.info {
            "========================================="
        }

        Log.logger.info {
            AppConfig.APPLICATION_NAME
        }

        Log.logger.info {
            "Version ${AppConfig.APPLICATION_VERSION}"
        }

        Log.logger.info {
            "========================================="
        }

        services.fileSystemService
            .initialize()

        services.databaseService
            .initialize()

        CsvImportService(
            services.databaseService
        )
            .verifyInputFile()

        AnalyticsPipeline()

            .add(
                OffenseStatisticsModule(
                    services.databaseService
                )
            )

            .add(
                NeighborhoodStatisticsModule(
                    services.databaseService
                )
            )

            .add(
                ZipCodeStatisticsModule(
                    services.databaseService
                )
            )

            .add(
                HourStatisticsModule(
                    services.databaseService
                )
            )

            .add(
                DayOfWeekStatisticsModule(
                    services.databaseService
                )
            )

            .add(
                MonthStatisticsModule(
                    services.databaseService
                )
            )

            .add(
                YearStatisticsModule(
                    services.databaseService
                )
            )

            .add(
                PrecinctStatisticsModule(
                    services.databaseService
                )
            )

            .add(
                CouncilDistrictStatisticsModule(
                    services.databaseService
                )
            )

            .add(
                HeatmapModule(
                    services.databaseService
                )
            )

            .add(
                NoiseModule()
            )

            .add(
                Section8Module()
            )

            .add(
                LiquorStoreModule()
            )

            .add(
                DollarStoreModule()
            )

            .add(
                MedianIncomeModule(
                    services.databaseService
                )
            )

            .add(
                VacantPropertyModule()
            )

            .add(
                SearchIndexModule(
                    services.databaseService
                )
            )

            .add(
                DatabaseStatisticsModule(
                    services.databaseService
                )
            )

            /*
             * -------------------------------------------------
             * PHASE 2 — REGIONAL TILE MANIFEST
             * -------------------------------------------------
             *
             * Run this AFTER all existing exporters so it can
             * inspect the completed JSON outputs and determine
             * which geographic tiles are represented.
             */
            .add(
                RegionalTileModule()
            )

            .run()

        Log.logger.info {
            "Application started successfully."
        }

        services.databaseService
            .shutdown()
    }
}