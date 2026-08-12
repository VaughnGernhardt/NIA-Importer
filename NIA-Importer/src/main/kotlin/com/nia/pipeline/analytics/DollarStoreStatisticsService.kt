package com.nia.pipeline.analytics

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import com.nia.pipeline.analytics.model.DollarStore
import com.nia.pipeline.logging.Log
import org.jsoup.Jsoup
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class DollarStoreStatisticsService {

    companion object {

        private const val DOLLAR_GENERAL_STATE_URL =
            "https://www.dollargeneral.com/store-directory/mi"

        private const val DOLLAR_TREE_STATE_URL =
            "https://locations.dollartree.com/mi"

        private const val FAMILY_DOLLAR_STATE_URL =
            "https://locations.familydollar.com/mi"

        private const val CENSUS_GEOCODER =
            "https://geocoding.geo.census.gov/geocoder/locations/onelineaddress"

        private const val CACHE_FILE =
            "data/cache/dollar_store_geocodes_michigan.json"

        /*
         * Approximate Michigan bounds covering both peninsulas.
         */
        private const val MICHIGAN_MIN_LAT =
            41.60

        private const val MICHIGAN_MAX_LAT =
            48.35

        private const val MICHIGAN_MIN_LON =
            -90.50

        private const val MICHIGAN_MAX_LON =
            -82.10

        private const val REQUEST_DELAY_MS =
            40L

        private const val GEOCODE_DELAY_MS =
            75L
    }

    private val gson =
        Gson()

    private data class GeocodeResult(

        val latitude: Double,

        val longitude: Double
    )

    private data class RawDollarStore(

        val storeId: String,

        val storeName: String,

        val chain: String,

        val address: String,

        val city: String,

        val zipCode: String
    )

    fun generate(): List<DollarStore> {

        Log.logger.info {
            "Loading Michigan dollar store directories..."
        }

        val rawStores =
            mutableListOf<RawDollarStore>()

        val dollarGeneral =
            loadDollarGeneralStores()

        Log.logger.info {
            "Michigan Dollar General stores discovered: ${dollarGeneral.size}"
        }

        rawStores.addAll(
            dollarGeneral
        )

        val dollarTree =
            loadDollarTreeStores()

        Log.logger.info {
            "Michigan Dollar Tree stores discovered: ${dollarTree.size}"
        }

        rawStores.addAll(
            dollarTree
        )

        val familyDollar =
            loadFamilyDollarStores()

        Log.logger.info {
            "Michigan Family Dollar stores discovered: ${familyDollar.size}"
        }

        rawStores.addAll(
            familyDollar
        )

        val uniqueStores =
            rawStores
                .filter {
                    it.address.isNotBlank()
                }
                .distinctBy {
                    "${it.chain}|${normalizeAddress(it.address)}"
                }

        Log.logger.info {
            "Unique Michigan dollar stores before geocoding: ${uniqueStores.size}"
        }

        val geocodeCache =
            loadGeocodeCache()

        Log.logger.info {
            "Cached Michigan dollar store geocodes: ${geocodeCache.size}"
        }

        val results =
            mutableListOf<DollarStore>()

        var cacheHits =
            0

        var geocodeSuccesses =
            0

        var geocodeFailures =
            0

        uniqueStores
            .forEachIndexed {
                    index,
                    store ->

                Log.logger.info {
                    "Dollar store ${index + 1}/${uniqueStores.size}: " +
                            "${store.chain} - ${store.address}"
                }

                val cacheKey =
                    normalizeAddress(
                        store.address
                    )

                val cached =
                    geocodeCache[
                        cacheKey
                    ]

                val coordinates =
                    if (
                        cached != null
                    ) {

                        cacheHits++

                        cached

                    } else {

                        val result =
                            geocodeAddress(
                                store.address
                            )

                        if (
                            result != null
                        ) {

                            geocodeSuccesses++

                            geocodeCache[
                                cacheKey
                            ] =
                                result

                            if (
                                geocodeSuccesses % 25 == 0
                            ) {

                                saveGeocodeCache(
                                    geocodeCache
                                )
                            }

                        } else {

                            geocodeFailures++
                        }

                        Thread.sleep(
                            GEOCODE_DELAY_MS
                        )

                        result
                    }

                if (
                    coordinates == null
                ) {

                    return@forEachIndexed
                }

                if (
                    !isInsideMichigan(
                        latitude =
                            coordinates.latitude,

                        longitude =
                            coordinates.longitude
                    )
                ) {

                    return@forEachIndexed
                }

                results.add(
                    DollarStore(

                        storeId =
                            store.storeId,

                        storeName =
                            store.storeName,

                        chain =
                            store.chain,

                        address =
                            store.address,

                        city =
                            store.city,

                        zipCode =
                            store.zipCode,

                        latitude =
                            coordinates.latitude,

                        longitude =
                            coordinates.longitude
                    )
                )
            }

        saveGeocodeCache(
            geocodeCache
        )

        val finalResults =
            results
                .distinctBy {
                    "${it.chain}|${normalizeAddress(it.address)}"
                }
                .sortedWith(
                    compareBy<DollarStore> {
                        it.chain
                    }
                        .thenBy {
                            it.city
                        }
                        .thenBy {
                            it.address
                        }
                )

        Log.logger.info {
            "Michigan dollar store generation complete."
        }

        Log.logger.info {
            "Dollar store geocode cache hits: $cacheHits"
        }

        Log.logger.info {
            "Dollar store new geocode successes: $geocodeSuccesses"
        }

        Log.logger.info {
            "Dollar store geocode failures: $geocodeFailures"
        }

        Log.logger.info {
            "Michigan dollar stores exported: ${finalResults.size}"
        }

        return finalResults
    }

    /*
     * ---------------------------------------------------------
     * DOLLAR GENERAL
     * ---------------------------------------------------------
     */

    private fun loadDollarGeneralStores():
            List<RawDollarStore> {

        val stateDocument =
            Jsoup
                .connect(
                    DOLLAR_GENERAL_STATE_URL
                )
                .userAgent(
                    "Mozilla/5.0 NIA-Importer"
                )
                .timeout(
                    30_000
                )
                .get()

        val cityUrls =
            stateDocument
                .select(
                    "a[href*='/store-directory/mi/']"
                )
                .map {
                    it.absUrl(
                        "href"
                    )
                }
                .filter {
                    it.isNotBlank()
                }
                .filter {
                    it.startsWith(
                        "$DOLLAR_GENERAL_STATE_URL/",
                        ignoreCase =
                            true
                    )
                }
                .filter {
                    cityPathDepth(
                        url =
                            it,
                        prefix =
                            "/store-directory/mi/"
                    ) ==
                            1
                }
                .distinct()

        Log.logger.info {
            "Dollar General Michigan city pages: ${cityUrls.size}"
        }

        val stores =
            mutableListOf<RawDollarStore>()

        cityUrls
            .forEachIndexed {
                    index,
                    cityUrl ->

                Log.logger.info {
                    "Dollar General city ${index + 1}/${cityUrls.size}: $cityUrl"
                }

                try {

                    val cityDocument =
                        Jsoup
                            .connect(
                                cityUrl
                            )
                            .userAgent(
                                "Mozilla/5.0 NIA-Importer"
                            )
                            .timeout(
                                30_000
                            )
                            .get()

                    val storeLinks =
                        cityDocument
                            .select(
                                "a[href*='/store-directory/mi/']"
                            )
                            .map {
                                it.absUrl(
                                    "href"
                                )
                            }
                            .filter {
                                it.startsWith(
                                    "$DOLLAR_GENERAL_STATE_URL/",
                                    ignoreCase =
                                        true
                                )
                            }
                            .filter {
                                cityPathDepth(
                                    url =
                                        it,
                                    prefix =
                                        "/store-directory/mi/"
                                ) >=
                                        2
                            }
                            .distinct()

                    if (
                        storeLinks.isEmpty()
                    ) {

                        /*
                         * Some DG city pages contain the complete
                         * addresses directly in the city page.
                         */
                        stores.addAll(
                            extractStoresFromDirectoryPage(
                                text =
                                    cityDocument.text(),

                                chain =
                                    "Dollar General",

                                idPrefix =
                                    "DG"
                            )
                        )

                    } else {

                        storeLinks.forEach {
                                storeUrl ->

                            try {

                                val storeDocument =
                                    Jsoup
                                        .connect(
                                            storeUrl
                                        )
                                        .userAgent(
                                            "Mozilla/5.0 NIA-Importer"
                                        )
                                        .timeout(
                                            30_000
                                        )
                                        .get()

                                val parsed =
                                    extractMichiganStore(
                                        text =
                                            storeDocument.text(),

                                        chain =
                                            "Dollar General",

                                        storeId =
                                            storeUrl
                                                .substringAfterLast(
                                                    "/"
                                                )
                                                .substringBefore(
                                                    "?"
                                                )
                                    )

                                if (
                                    parsed != null
                                ) {

                                    stores.add(
                                        parsed
                                    )
                                }

                                Thread.sleep(
                                    REQUEST_DELAY_MS
                                )

                            } catch (
                                exception: Exception
                            ) {

                                Log.logger.warn {
                                    "Unable to read Dollar General store: $storeUrl"
                                }
                            }
                        }
                    }

                    Thread.sleep(
                        REQUEST_DELAY_MS
                    )

                } catch (
                    exception: Exception
                ) {

                    Log.logger.warn {
                        "Unable to read Dollar General city page: $cityUrl"
                    }
                }
            }

        return stores
            .distinctBy {
                "${it.chain}|${normalizeAddress(it.address)}"
            }
    }

    /*
     * ---------------------------------------------------------
     * DOLLAR TREE
     * ---------------------------------------------------------
     */

    private fun loadDollarTreeStores():
            List<RawDollarStore> {

        return loadDollarTreeFamilyDollarState(
            stateUrl =
                DOLLAR_TREE_STATE_URL,

            chain =
                "Dollar Tree"
        )
    }

    /*
     * ---------------------------------------------------------
     * FAMILY DOLLAR
     * ---------------------------------------------------------
     */

    private fun loadFamilyDollarStores():
            List<RawDollarStore> {

        return loadDollarTreeFamilyDollarState(
            stateUrl =
                FAMILY_DOLLAR_STATE_URL,

            chain =
                "Family Dollar"
        )
    }

    /*
     * ---------------------------------------------------------
     * DOLLAR TREE / FAMILY DOLLAR STATE CRAWLER
     * ---------------------------------------------------------
     */

    private fun loadDollarTreeFamilyDollarState(
        stateUrl: String,
        chain: String
    ): List<RawDollarStore> {

        val stateDocument =
            Jsoup
                .connect(
                    stateUrl
                )
                .userAgent(
                    "Mozilla/5.0 NIA-Importer"
                )
                .timeout(
                    30_000
                )
                .get()

        val cityUrls =
            stateDocument
                .select(
                    "a[href]"
                )
                .map {
                    it.absUrl(
                        "href"
                    )
                }
                .filter {
                    it.startsWith(
                        "$stateUrl/",
                        ignoreCase =
                            true
                    )
                }
                .filter {
                    cityPathDepth(
                        url =
                            it,
                        prefix =
                            "/mi/"
                    ) ==
                            1
                }
                .distinct()

        Log.logger.info {
            "$chain Michigan city pages: ${cityUrls.size}"
        }

        val stores =
            mutableListOf<RawDollarStore>()

        cityUrls
            .forEachIndexed {
                    index,
                    cityUrl ->

                Log.logger.info {
                    "$chain city ${index + 1}/${cityUrls.size}: $cityUrl"
                }

                try {

                    val cityDocument =
                        Jsoup
                            .connect(
                                cityUrl
                            )
                            .userAgent(
                                "Mozilla/5.0 NIA-Importer"
                            )
                            .timeout(
                                30_000
                            )
                            .get()

                    val storeLinks =
                        cityDocument
                            .select(
                                "a[href]"
                            )
                            .map {
                                it.absUrl(
                                    "href"
                                )
                            }
                            .filter {
                                it.startsWith(
                                    "$stateUrl/",
                                    ignoreCase =
                                        true
                                )
                            }
                            .filter {
                                cityPathDepth(
                                    url =
                                        it,
                                    prefix =
                                        "/mi/"
                                ) >=
                                        2
                            }
                            .distinct()

                    if (
                        storeLinks.isEmpty()
                    ) {

                        stores.addAll(
                            extractStoresFromDirectoryPage(
                                text =
                                    cityDocument.text(),

                                chain =
                                    chain,

                                idPrefix =
                                    if (
                                        chain ==
                                        "Dollar Tree"
                                    ) {
                                        "DT"
                                    } else {
                                        "FD"
                                    }
                            )
                        )

                    } else {

                        storeLinks
                            .forEach {
                                    storeUrl ->

                                try {

                                    val storeDocument =
                                        Jsoup
                                            .connect(
                                                storeUrl
                                            )
                                            .userAgent(
                                                "Mozilla/5.0 NIA-Importer"
                                            )
                                            .timeout(
                                                30_000
                                            )
                                            .get()

                                    val parsed =
                                        extractMichiganStore(
                                            text =
                                                storeDocument.text(),

                                            chain =
                                                chain,

                                            storeId =
                                                storeUrl
                                                    .substringAfterLast(
                                                        "/"
                                                    )
                                                    .substringBefore(
                                                        "?"
                                                    )
                                        )

                                    if (
                                        parsed != null
                                    ) {

                                        stores.add(
                                            parsed
                                        )
                                    }

                                    Thread.sleep(
                                        REQUEST_DELAY_MS
                                    )

                                } catch (
                                    exception: Exception
                                ) {

                                    Log.logger.warn {
                                        "Unable to read $chain store: $storeUrl"
                                    }
                                }
                            }
                    }

                    Thread.sleep(
                        REQUEST_DELAY_MS
                    )

                } catch (
                    exception: Exception
                ) {

                    Log.logger.warn {
                        "Unable to read $chain city page: $cityUrl"
                    }
                }
            }

        return stores
            .distinctBy {
                "${it.chain}|${normalizeAddress(it.address)}"
            }
    }

    /*
     * ---------------------------------------------------------
     * EXTRACT ONE STORE
     * ---------------------------------------------------------
     */

    private fun extractMichiganStore(
        text: String,
        chain: String,
        storeId: String
    ): RawDollarStore? {

        val normalized =
            text
                .replace(
                    Regex(
                        """\s+"""
                    ),
                    " "
                )
                .trim()

        val regex =
            Regex(
                """([0-9A-Za-z][0-9A-Za-z .#'&/\-]{2,100}?)\s+""" +
                        """([A-Za-z][A-Za-z .'\-]{1,60}),\s*""" +
                        """MI(?:\s*\(Michigan\))?,?\s*""" +
                        """(4[89]\d{3}(?:-\d{4})?)""",
                RegexOption.IGNORE_CASE
            )

        val match =
            regex.find(
                normalized
            )
                ?: return null

        val street =
            cleanStreetAddress(
                match
                    .groupValues[
                    1
                ]
            )

        val city =
            cleanCity(
                match
                    .groupValues[
                    2
                ]
            )

        val fullZip =
            match
                .groupValues[
                3
            ]
                .trim()

        val zipCode =
            fullZip
                .take(
                    5
                )

        if (
            street.isBlank() ||
            city.isBlank()
        ) {

            return null
        }

        val address =
            "$street, $city, MI $fullZip"

        return RawDollarStore(

            storeId =
                storeId
                    .ifBlank {
                        "${chain.hashCode()}-${normalizeAddress(address)}"
                    },

            storeName =
                chain,

            chain =
                chain,

            address =
                address,

            city =
                city,

            zipCode =
                zipCode
        )
    }

    /*
     * ---------------------------------------------------------
     * EXTRACT MULTIPLE STORES FROM CITY PAGE
     * ---------------------------------------------------------
     */

    private fun extractStoresFromDirectoryPage(
        text: String,
        chain: String,
        idPrefix: String
    ): List<RawDollarStore> {

        val normalized =
            text
                .replace(
                    Regex(
                        """\s+"""
                    ),
                    " "
                )
                .trim()

        val regex =
            Regex(
                """([0-9A-Za-z][0-9A-Za-z .#'&/\-]{2,100}?)\s+""" +
                        """([A-Za-z][A-Za-z .'\-]{1,60}),\s*""" +
                        """MI(?:\s*\(Michigan\))?,?\s*""" +
                        """(4[89]\d{3}(?:-\d{4})?)""",
                RegexOption.IGNORE_CASE
            )

        return regex
            .findAll(
                normalized
            )
            .mapIndexedNotNull {
                    index,
                    match ->

                val street =
                    cleanStreetAddress(
                        match.groupValues[1]
                    )

                val city =
                    cleanCity(
                        match.groupValues[2]
                    )

                val fullZip =
                    match
                        .groupValues[
                        3
                    ]
                        .trim()

                if (
                    street.isBlank() ||
                    city.isBlank()
                ) {

                    return@mapIndexedNotNull null
                }

                val address =
                    "$street, $city, MI $fullZip"

                RawDollarStore(

                    storeId =
                        "$idPrefix-${normalizeAddress(address)}-$index",

                    storeName =
                        chain,

                    chain =
                        chain,

                    address =
                        address,

                    city =
                        city,

                    zipCode =
                        fullZip.take(
                            5
                        )
                )
            }
            .distinctBy {
                "${it.chain}|${normalizeAddress(it.address)}"
            }
            .toList()
    }

    private fun cleanStreetAddress(
        value: String
    ): String {

        var street =
            value
                .replace(
                    Regex(
                        """\s+"""
                    ),
                    " "
                )
                .trim()

        val unwantedPrefixes =
            listOf(
                "Get Directions View Store Page",
                "View Store Details",
                "Store Hours",
                "Open Now",
                "Closed",
                "Dollar General",
                "Dollar Tree",
                "Family Dollar"
            )

        unwantedPrefixes
            .forEach {
                    prefix ->

                val index =
                    street
                        .lastIndexOf(
                            prefix,
                            ignoreCase =
                                true
                        )

                if (
                    index >= 0
                ) {

                    street =
                        street
                            .substring(
                                index + prefix.length
                            )
                            .trim()
                }
            }

        /*
         * Keep the address beginning with the final plausible
         * street-number token.
         */
        val addressStart =
            Regex(
                """\b[0-9]{1,6}[A-Za-z]?\s"""
            )
                .findAll(
                    street
                )
                .lastOrNull()

        if (
            addressStart != null
        ) {

            street =
                street
                    .substring(
                        addressStart.range.first
                    )
                    .trim()
        }

        return street
    }

    private fun cleanCity(
        value: String
    ): String {

        return value
            .replace(
                Regex(
                    """\s+"""
                ),
                " "
            )
            .trim()
    }

    private fun cityPathDepth(
        url: String,
        prefix: String
    ): Int {

        val path =
            try {

                URI(
                    url
                )
                    .path

            } catch (
                exception: Exception
            ) {

                return 0
            }

        val remainder =
            path
                .substringAfter(
                    prefix,
                    ""
                )
                .trim(
                    '/'
                )

        if (
            remainder.isBlank()
        ) {

            return 0
        }

        return remainder
            .split(
                "/"
            )
            .count {
                it.isNotBlank()
            }
    }

    private fun geocodeAddress(
        address: String
    ): GeocodeResult? {

        val encodedAddress =
            URLEncoder.encode(
                address,
                StandardCharsets.UTF_8
            )

        val url =
            "$CENSUS_GEOCODER" +
                    "?address=$encodedAddress" +
                    "&benchmark=Public_AR_Current" +
                    "&format=json"

        val response =
            try {

                downloadText(
                    url
                )

            } catch (
                exception: Exception
            ) {

                return null
            }

        return try {

            val root =
                JsonParser
                    .parseString(
                        response
                    )
                    .asJsonObject

            val result =
                root
                    .getAsJsonObject(
                        "result"
                    )
                    ?: return null

            val matches =
                result
                    .getAsJsonArray(
                        "addressMatches"
                    )
                    ?: return null

            if (
                matches.size() ==
                0
            ) {

                return null
            }

            val firstMatch =
                matches[
                    0
                ]
                    .asJsonObject

            val coordinates =
                firstMatch
                    .getAsJsonObject(
                        "coordinates"
                    )
                    ?: return null

            val longitude =
                coordinates
                    .get(
                        "x"
                    )
                    ?.takeUnless {
                        it.isJsonNull
                    }
                    ?.asDouble
                    ?: return null

            val latitude =
                coordinates
                    .get(
                        "y"
                    )
                    ?.takeUnless {
                        it.isJsonNull
                    }
                    ?.asDouble
                    ?: return null

            if (
                !isInsideMichigan(
                    latitude =
                        latitude,

                    longitude =
                        longitude
                )
            ) {

                return null
            }

            GeocodeResult(

                latitude =
                    latitude,

                longitude =
                    longitude
            )

        } catch (
            exception: Exception
        ) {

            null
        }
    }

    private fun loadGeocodeCache():
            MutableMap<String, GeocodeResult> {

        val file =
            File(
                CACHE_FILE
            )

        if (
            !file.exists()
        ) {

            return mutableMapOf()
        }

        return try {

            val type =
                object :
                    TypeToken<
                            MutableMap<
                                    String,
                                    GeocodeResult
                                    >
                            >() {}
                    .type

            gson.fromJson<
                    MutableMap<
                            String,
                            GeocodeResult
                            >
                    >(
                file.readText(),
                type
            )
                ?: mutableMapOf()

        } catch (
            exception: Exception
        ) {

            mutableMapOf()
        }
    }

    private fun saveGeocodeCache(
        cache: Map<String, GeocodeResult>
    ) {

        val file =
            File(
                CACHE_FILE
            )

        file
            .parentFile
            ?.mkdirs()

        file.writeText(
            gson.toJson(
                cache
            )
        )
    }

    private fun normalizeAddress(
        address: String
    ): String {

        return address
            .uppercase()
            .replace(
                Regex(
                    "[^A-Z0-9]"
                ),
                ""
            )
    }

    private fun isInsideMichigan(
        latitude: Double,
        longitude: Double
    ): Boolean {

        return latitude in
                MICHIGAN_MIN_LAT..MICHIGAN_MAX_LAT &&
                longitude in
                MICHIGAN_MIN_LON..MICHIGAN_MAX_LON
    }

    private fun downloadText(
        url: String
    ): String {

        val connection =
            URI(
                url
            )
                .toURL()
                .openConnection() as
                    HttpURLConnection

        connection.requestMethod =
            "GET"

        connection.connectTimeout =
            30_000

        connection.readTimeout =
            30_000

        connection.instanceFollowRedirects =
            true

        connection.setRequestProperty(
            "User-Agent",
            "Mozilla/5.0 NIA-Importer"
        )

        try {

            val responseCode =
                connection.responseCode

            if (
                responseCode !in
                200..299
            ) {

                throw IllegalStateException(
                    "HTTP request failed with $responseCode for $url"
                )
            }

            return connection
                .inputStream
                .bufferedReader()
                .use {
                    it.readText()
                }

        } finally {

            connection.disconnect()
        }
    }
}