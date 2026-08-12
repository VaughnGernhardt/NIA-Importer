package com.nia.pipeline.regional

import kotlin.math.floor

object RegionalTileCalculator {

    /*
     * ---------------------------------------------------------
     * TILE SIZE
     * ---------------------------------------------------------
     *
     * 0.10 degrees latitude x 0.10 degrees longitude.
     *
     * This gives us tiles that are small enough for efficient
     * regional loading while avoiding an excessive number
     * of tiny files.
     */

    const val TILE_SIZE_DEGREES =
        0.10

    /*
     * ---------------------------------------------------------
     * TILE DESCRIPTION
     * ---------------------------------------------------------
     */

    data class TileBounds(

        val id: String,

        val south: Double,

        val west: Double,

        val north: Double,

        val east: Double
    )

    /*
     * ---------------------------------------------------------
     * CALCULATE TILE FOR COORDINATE
     * ---------------------------------------------------------
     *
     * Every latitude / longitude pair will always produce the
     * same tile.
     *
     * Example:
     *
     * latitude  = 42.3314
     * longitude = -83.0458
     *
     * becomes approximately:
     *
     * south = 42.3
     * west  = -83.1
     *
     * tile id:
     *
     * 42_3_-83_1
     */

    fun tileForCoordinate(
        latitude: Double,
        longitude: Double
    ): TileBounds {

        require(
            latitude in -90.0..90.0
        ) {
            "Latitude must be between -90 and 90."
        }

        require(
            longitude in -180.0..180.0
        ) {
            "Longitude must be between -180 and 180."
        }

        val south =
            floorToTileBoundary(
                latitude
            )

        val west =
            floorToTileBoundary(
                longitude
            )

        val north =
            normalizeCoordinate(
                south +
                        TILE_SIZE_DEGREES
            )

        val east =
            normalizeCoordinate(
                west +
                        TILE_SIZE_DEGREES
            )

        return TileBounds(
            id =
                buildTileId(
                    south =
                        south,

                    west =
                        west
                ),

            south =
                south,

            west =
                west,

            north =
                north,

            east =
                east
        )
    }

    /*
     * ---------------------------------------------------------
     * TILE ID
     * ---------------------------------------------------------
     *
     * IDs are filesystem safe.
     *
     * Examples:
     *
     * 42.3  -> 42_3
     * -83.1 -> -83_1
     *
     * Final:
     *
     * 42_3_-83_1
     */

    fun buildTileId(
        south: Double,
        west: Double
    ): String {

        return "${formatCoordinate(south)}_${formatCoordinate(west)}"
    }

    /*
     * ---------------------------------------------------------
     * TILE CONTAINS COORDINATE
     * ---------------------------------------------------------
     */

    fun contains(
        tile: TileBounds,
        latitude: Double,
        longitude: Double
    ): Boolean {

        return latitude >= tile.south &&
                latitude < tile.north &&
                longitude >= tile.west &&
                longitude < tile.east
    }

    /*
     * ---------------------------------------------------------
     * FLOOR TO TILE BOUNDARY
     * ---------------------------------------------------------
     *
     * Important for negative coordinates.
     *
     * Example:
     *
     * -83.0458 must become -83.1,
     * NOT -83.0.
     */

    private fun floorToTileBoundary(
        coordinate: Double
    ): Double {

        val tileIndex =
            floor(
                coordinate /
                        TILE_SIZE_DEGREES
            )

        return normalizeCoordinate(
            tileIndex *
                    TILE_SIZE_DEGREES
        )
    }

    /*
     * ---------------------------------------------------------
     * NORMALIZE FLOATING POINT VALUES
     * ---------------------------------------------------------
     *
     * Prevent values such as:
     *
     * 42.300000000000004
     *
     * from leaking into filenames and manifests.
     */

    private fun normalizeCoordinate(
        value: Double
    ): Double {

        return String.format(
            java.util.Locale.US,
            "%.1f",
            value
        )
            .toDouble()
    }

    /*
     * ---------------------------------------------------------
     * FILESYSTEM SAFE COORDINATE
     * ---------------------------------------------------------
     */

    private fun formatCoordinate(
        value: Double
    ): String {

        return String.format(
            java.util.Locale.US,
            "%.1f",
            value
        )
            .replace(
                ".",
                "_"
            )
    }
}