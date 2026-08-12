package com.nia.pipeline.analytics

import com.google.gson.JsonParser
import com.nia.pipeline.analytics.model.VacantProperty
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class VacantPropertyStatisticsService {

    companion object {

        private const val QUERY_URL =
            "https://services2.arcgis.com/qvkbeam7Wirps6zC/arcgis/rest/services/" +
                    "bseed_vacant_property_registrations/FeatureServer/0/query"

        private const val PAGE_SIZE =
            1000
    }

    fun generate(): List<VacantProperty> {

        val properties =
            mutableListOf<VacantProperty>()

        var offset =
            0

        while (true) {

            println(
                "Downloading vacant properties starting at record $offset..."
            )

            val page =
                downloadPage(
                    offset = offset
                )

            if (page.isEmpty()) {
                break
            }

            properties.addAll(
                page
            )

            if (page.size < PAGE_SIZE) {
                break
            }

            offset +=
                PAGE_SIZE
        }

        return properties
            .filter {
                it.latitude in 42.0..43.0 &&
                        it.longitude in -84.0..-82.0
            }
            .distinctBy {
                it.registrationId.ifBlank {
                    "${it.address}|${it.latitude}|${it.longitude}"
                }
            }
            .sortedBy {
                it.address
            }
    }

    private fun downloadPage(
        offset: Int
    ): List<VacantProperty> {

        val parameters =
            linkedMapOf(
                "where" to "1=1",

                "outFields" to
                        "record_id," +
                        "address," +
                        "latitude," +
                        "longitude",

                "returnGeometry" to "false",

                "orderByFields" to
                        "ObjectId ASC",

                "resultOffset" to
                        offset.toString(),

                "resultRecordCount" to
                        PAGE_SIZE.toString(),

                "f" to "json"
            )

        val queryString =
            parameters.entries
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
                "$QUERY_URL?$queryString"
            ).toURL()

        val connection =
            url.openConnection() as HttpURLConnection

        connection.requestMethod =
            "GET"

        connection.connectTimeout =
            30_000

        connection.readTimeout =
            60_000

        connection.instanceFollowRedirects =
            true

        connection.setRequestProperty(
            "Accept",
            "application/json"
        )

        connection.setRequestProperty(
            "User-Agent",
            "Mozilla/5.0 NIA-Importer"
        )

        try {

            val responseCode =
                connection.responseCode

            if (responseCode !in 200..299) {

                throw IllegalStateException(
                    "Detroit vacant property request failed " +
                            "with HTTP $responseCode"
                )
            }

            val response =
                connection.inputStream
                    .bufferedReader()
                    .use {
                        it.readText()
                    }

            val root =
                JsonParser.parseString(
                    response
                ).asJsonObject

            if (root.has("error")) {

                throw IllegalStateException(
                    "Detroit vacant property API returned an error: " +
                            root.get("error")
                )
            }

            val features =
                root.getAsJsonArray(
                    "features"
                )
                    ?: return emptyList()

            val properties =
                mutableListOf<VacantProperty>()

            features.forEach { featureElement ->

                val feature =
                    featureElement.asJsonObject

                val attributes =
                    feature.getAsJsonObject(
                        "attributes"
                    )
                        ?: return@forEach

                val registrationId =
                    attributes
                        .get("record_id")
                        ?.takeUnless {
                            it.isJsonNull
                        }
                        ?.asString
                        ?.trim()
                        ?: ""

                val address =
                    attributes
                        .get("address")
                        ?.takeUnless {
                            it.isJsonNull
                        }
                        ?.asString
                        ?.trim()
                        ?: ""

                val latitude =
                    attributes
                        .get("latitude")
                        ?.takeUnless {
                            it.isJsonNull
                        }
                        ?.asDouble
                        ?: return@forEach

                val longitude =
                    attributes
                        .get("longitude")
                        ?.takeUnless {
                            it.isJsonNull
                        }
                        ?.asDouble
                        ?: return@forEach

                if (address.isBlank()) {
                    return@forEach
                }

                properties.add(
                    VacantProperty(
                        registrationId =
                            registrationId,

                        address =
                            address,

                        latitude =
                            latitude,

                        longitude =
                            longitude,

                        status =
                            "Registered Vacant"
                    )
                )
            }

            return properties

        } finally {

            connection.disconnect()
        }
    }
}