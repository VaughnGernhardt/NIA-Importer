package com.nia.pipeline.analytics

import com.google.gson.JsonParser
import com.nia.pipeline.analytics.model.Section8Property
import com.nia.pipeline.logging.Log
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class Section8StatisticsService {

    companion object {

        private const val HUD_QUERY_URL =
            "https://egis.hud.gov/arcgis/rest/services/cpdmaps/HudMfProps/MapServer/2/query"

        /*
         * Approximate Michigan bounding box.
         *
         * Covers both peninsulas.
         */
        private const val MICHIGAN_MIN_LAT =
            41.60

        private const val MICHIGAN_MAX_LAT =
            48.35

        private const val MICHIGAN_MIN_LON =
            -90.50

        private const val MICHIGAN_MAX_LON =
            -82.10

        /*
         * HUD service MaxRecordCount is 1000.
         */
        private const val PAGE_SIZE =
            1000
    }

    fun generate(): List<Section8Property> {

        Log.logger.info {
            "Loading Michigan HUD assisted multifamily properties..."
        }

        val results =
            mutableListOf<Section8Property>()

        var offset =
            0

        while (
            true
        ) {

            Log.logger.info {
                "Requesting HUD Section 8 page at offset $offset..."
            }

            val page =
                loadPage(
                    offset =
                        offset
                )

            results.addAll(
                page.properties
            )

            Log.logger.info {
                "HUD page returned ${page.properties.size} properties."
            }

            if (
                !page.exceededTransferLimit &&
                page.properties.size < PAGE_SIZE
            ) {

                break
            }

            if (
                page.properties.isEmpty()
            ) {

                break
            }

            offset +=
                PAGE_SIZE
        }

        val uniqueProperties =
            results
                .filter { property ->

                    property.latitude in
                            MICHIGAN_MIN_LAT..MICHIGAN_MAX_LAT &&
                            property.longitude in
                            MICHIGAN_MIN_LON..MICHIGAN_MAX_LON
                }
                .distinctBy { property ->

                    if (
                        property.propertyId.isNotBlank()
                    ) {

                        property.propertyId

                    } else {

                        "${property.propertyName}|" +
                                "${property.latitude}|" +
                                "${property.longitude}"
                    }
                }
                .sortedBy {
                    it.propertyName
                }

        Log.logger.info {
            "Michigan HUD assisted properties generated: ${uniqueProperties.size}"
        }

        return uniqueProperties
    }

    private fun loadPage(
        offset: Int
    ): HudPage {

        val geometry =
            "$MICHIGAN_MIN_LON," +
                    "$MICHIGAN_MIN_LAT," +
                    "$MICHIGAN_MAX_LON," +
                    "$MICHIGAN_MAX_LAT"

        val parameters =
            linkedMapOf(

                "where" to
                        "1=1",

                "geometry" to
                        geometry,

                "geometryType" to
                        "esriGeometryEnvelope",

                "inSR" to
                        "4326",

                "spatialRel" to
                        "esriSpatialRelIntersects",

                "outFields" to
                        "PROPERTY_ID," +
                        "PROPERTY_NAME_TEXT," +
                        "TOTAL_ASSISTED_UNIT_COUNT," +
                        "TOTAL_UNIT_COUNT",

                "returnGeometry" to
                        "true",

                "outSR" to
                        "4326",

                "resultOffset" to
                        offset.toString(),

                "resultRecordCount" to
                        PAGE_SIZE.toString(),

                "returnExceededLimitFeatures" to
                        "true",

                "f" to
                        "json"
            )

        val queryString =
            parameters
                .entries
                .joinToString(
                    "&"
                ) { entry ->

                    val key =
                        URLEncoder.encode(
                            entry.key,
                            StandardCharsets.UTF_8
                        )

                    val value =
                        URLEncoder.encode(
                            entry.value,
                            StandardCharsets.UTF_8
                        )

                    "$key=$value"
                }

        val url =
            URI(
                "$HUD_QUERY_URL?$queryString"
            )
                .toURL()

        val connection =
            url
                .openConnection() as
                    HttpURLConnection

        connection.requestMethod =
            "GET"

        connection.connectTimeout =
            30_000

        connection.readTimeout =
            60_000

        connection.setRequestProperty(
            "Accept",
            "application/json"
        )

        connection.setRequestProperty(
            "User-Agent",
            "NIA-Data-Pipeline/1.0"
        )

        try {

            val responseCode =
                connection.responseCode

            if (
                responseCode !in 200..299
            ) {

                val errorText =
                    connection
                        .errorStream
                        ?.bufferedReader()
                        ?.use {
                            it.readText()
                        }
                        ?: ""

                throw IllegalStateException(
                    "HUD request failed with HTTP $responseCode. " +
                            errorText.take(
                                500
                            )
                )
            }

            val response =
                connection
                    .inputStream
                    .bufferedReader()
                    .use {
                        it.readText()
                    }

            val root =
                JsonParser
                    .parseString(
                        response
                    )
                    .asJsonObject

            if (
                root.has(
                    "error"
                )
            ) {

                throw IllegalStateException(
                    "HUD returned an error: ${
                        root.get(
                            "error"
                        )
                    }"
                )
            }

            val exceededTransferLimit =
                root
                    .get(
                        "exceededTransferLimit"
                    )
                    ?.takeUnless {
                        it.isJsonNull
                    }
                    ?.asBoolean
                    ?: false

            val features =
                root
                    .getAsJsonArray(
                        "features"
                    )
                    ?: return HudPage(
                        properties =
                            emptyList(),

                        exceededTransferLimit =
                            exceededTransferLimit
                    )

            val properties =
                mutableListOf<Section8Property>()

            features
                .forEach {
                        featureElement ->

                    if (
                        !featureElement.isJsonObject
                    ) {

                        return@forEach
                    }

                    val feature =
                        featureElement
                            .asJsonObject

                    val attributes =
                        feature
                            .getAsJsonObject(
                                "attributes"
                            )
                            ?: return@forEach

                    val geometryObject =
                        feature
                            .getAsJsonObject(
                                "geometry"
                            )
                            ?: return@forEach

                    val longitude =
                        geometryObject
                            .get(
                                "x"
                            )
                            ?.takeUnless {
                                it.isJsonNull
                            }
                            ?.asDouble
                            ?: return@forEach

                    val latitude =
                        geometryObject
                            .get(
                                "y"
                            )
                            ?.takeUnless {
                                it.isJsonNull
                            }
                            ?.asDouble
                            ?: return@forEach

                    if (
                        latitude !in
                        MICHIGAN_MIN_LAT..MICHIGAN_MAX_LAT ||
                        longitude !in
                        MICHIGAN_MIN_LON..MICHIGAN_MAX_LON
                    ) {

                        return@forEach
                    }

                    val propertyId =
                        attributes
                            .get(
                                "PROPERTY_ID"
                            )
                            ?.takeUnless {
                                it.isJsonNull
                            }
                            ?.asString
                            ?: ""

                    val propertyName =
                        attributes
                            .get(
                                "PROPERTY_NAME_TEXT"
                            )
                            ?.takeUnless {
                                it.isJsonNull
                            }
                            ?.asString
                            ?: ""

                    val assistedUnits =
                        attributes
                            .get(
                                "TOTAL_ASSISTED_UNIT_COUNT"
                            )
                            ?.takeUnless {
                                it.isJsonNull
                            }
                            ?.asInt
                            ?: 0

                    val totalUnits =
                        attributes
                            .get(
                                "TOTAL_UNIT_COUNT"
                            )
                            ?.takeUnless {
                                it.isJsonNull
                            }
                            ?.asInt
                            ?: 0

                    properties.add(
                        Section8Property(

                            propertyId =
                                propertyId,

                            propertyName =
                                propertyName,

                            latitude =
                                latitude,

                            longitude =
                                longitude,

                            assistedUnits =
                                assistedUnits,

                            totalUnits =
                                totalUnits
                        )
                    )
                }

            return HudPage(

                properties =
                    properties,

                exceededTransferLimit =
                    exceededTransferLimit
            )

        } finally {

            connection.disconnect()
        }
    }

    private data class HudPage(

        val properties:
        List<Section8Property>,

        val exceededTransferLimit:
        Boolean
    )
}