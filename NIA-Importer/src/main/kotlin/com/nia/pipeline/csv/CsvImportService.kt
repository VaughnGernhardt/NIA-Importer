package com.nia.pipeline.csv

import com.nia.pipeline.database.DatabaseService
import com.nia.pipeline.logging.Log
import com.nia.pipeline.model.CrimeIncident
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVRecord
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

class CsvImportService(
    private val databaseService: DatabaseService
) {

    companion object {

        private const val INPUT_FILE =
            "data/input/rms_crime_incidents.csv"
    }

    fun verifyInputFile() {

        val csvFile =
            File(
                INPUT_FILE
            )

        require(
            csvFile.exists()
        ) {
            "CSV file not found: ${csvFile.absolutePath}"
        }

        Log.logger.info {
            "Found crime CSV file: ${csvFile.absolutePath}"
        }

        csvFile
            .bufferedReader()
            .use { reader ->

                val parser =
                    CSVFormat.DEFAULT
                        .builder()
                        .setHeader()
                        .setSkipHeaderRecord(
                            true
                        )
                        .setIgnoreEmptyLines(
                            true
                        )
                        .setTrim(
                            false
                        )
                        .get()
                        .parse(
                            reader
                        )

                val headers =
                    parser
                        .headerMap
                        .keys
                        .toList()

                Log.logger.info {
                    "Crime CSV columns detected: ${headers.joinToString(", ")}"
                }

                val crimes =
                    parser
                        .asSequence()
                        .mapNotNull {
                                record ->

                            parseRecord(
                                record
                            )
                        }

                databaseService
                    .importCrimeIncidents(
                        crimes
                    )
            }
    }

    private fun parseRecord(
        record: CSVRecord
    ): CrimeIncident? {

        val latitude =
            readDouble(
                record,
                "latitude",
                "lat",
                "incident_latitude",
                "y"
            )
                ?: return null

        val longitude =
            readDouble(
                record,
                "longitude",
                "lon",
                "lng",
                "incident_longitude",
                "x"
            )
                ?: return null

        if (
            latitude !in -90.0..90.0 ||
            longitude !in -180.0..180.0
        ) {

            return null
        }

        val incidentNumber =
            readString(
                record,
                "incident_entry_id",
                "incident_number",
                "incident_id",
                "incidentid",
                "case_number",
                "case_id",
                "id"
            )
                .ifBlank {

                    buildSyntheticIncidentNumber(
                        record =
                            record,

                        latitude =
                            latitude,

                        longitude =
                            longitude
                    )
                }

        val offense =
            readString(
                record,
                "offense_category",
                "offense",
                "crime_category",
                "crime_type",
                "ucr_category",
                "nibrs_offense",
                "offense_type"
            )

        val offenseDescription =
            readString(
                record,
                "offense_description",
                "description",
                "offense_desc",
                "crime_description",
                "nibrs_description"
            )
                .ifBlank {
                    offense
                }

        val dateOccurred =
            readString(
                record,
                "incident_occurred_at",
                "date_occurred",
                "incident_date",
                "occurred_at",
                "datetime",
                "date_time",
                "date"
            )

        val parsedDateTime =
            parseDateTime(
                dateOccurred
            )

        val year =
            readInt(
                record,
                "incident_year",
                "year",
                "occurred_year"
            )
                ?: parsedDateTime
                    ?.year
                ?: return null

        val hour =
            readInt(
                record,
                "incident_hour_of_day",
                "hour_of_day",
                "hour",
                "occurred_hour"
            )
                ?: parsedDateTime
                    ?.hour
                ?: 0

        val day =
            readInt(
                record,
                "incident_day_of_week",
                "day_of_week",
                "weekday",
                "occurred_day_of_week"
            )
                ?: parsedDateTime
                    ?.dayOfWeek
                    ?.toNiaDayNumber()
                ?: 0

        val address =
            readString(
                record,
                "nearest_intersection",
                "address",
                "location",
                "street_address",
                "incident_address"
            )

        val precinct =
            readString(
                record,
                "police_precinct",
                "precinct",
                "district",
                "police_district"
            )

        val zipCode =
            readString(
                record,
                "zip_code",
                "zipcode",
                "zip",
                "postal_code"
            )
                .take(
                    5
                )

        val neighborhood =
            readString(
                record,
                "neighborhood",
                "neighbourhood",
                "community",
                "community_name"
            )

        val councilDistrict =
            readString(
                record,
                "council_district",
                "district_number",
                "council"
            )

        return CrimeIncident(

            incidentNumber =
                incidentNumber,

            offense =
                offense,

            offenseDescription =
                offenseDescription,

            dateOccurred =
                dateOccurred,

            year =
                year,

            latitude =
                latitude,

            longitude =
                longitude,

            address =
                address,

            precinct =
                precinct,

            zipCode =
                zipCode,

            neighborhood =
                neighborhood,

            councilDistrict =
                councilDistrict,

            hourOfDay =
                hour
                    .coerceIn(
                        0,
                        23
                    ),

            dayOfWeek =
                day
                    .coerceIn(
                        0,
                        7
                    )
        )
    }

    private fun readString(
        record: CSVRecord,
        vararg possibleNames: String
    ): String {

        possibleNames
            .forEach {
                    name ->

                val actualHeader =
                    record
                        .parser
                        .headerMap
                        .keys
                        .firstOrNull {
                                header ->

                            header.equals(
                                name,
                                ignoreCase =
                                    true
                            )
                        }
                        ?: return@forEach

                val value =
                    try {

                        record[
                            actualHeader
                        ]

                    } catch (
                        exception: Exception
                    ) {

                        ""
                    }

                if (
                    value.isNotBlank()
                ) {

                    return value
                        .trim()
                }
            }

        return ""
    }

    private fun readInt(
        record: CSVRecord,
        vararg possibleNames: String
    ): Int? {

        val value =
            readString(
                record,
                *possibleNames
            )

        return value
            .toIntOrNull()
    }

    private fun readDouble(
        record: CSVRecord,
        vararg possibleNames: String
    ): Double? {

        val value =
            readString(
                record,
                *possibleNames
            )

        return value
            .toDoubleOrNull()
    }

    private fun buildSyntheticIncidentNumber(
        record: CSVRecord,
        latitude: Double,
        longitude: Double
    ): String {

        val date =
            readString(
                record,
                "incident_occurred_at",
                "date_occurred",
                "incident_date",
                "occurred_at",
                "datetime",
                "date_time",
                "date"
            )

        val offense =
            readString(
                record,
                "offense_category",
                "offense",
                "crime_category",
                "crime_type",
                "ucr_category",
                "nibrs_offense"
            )

        val source =
            "$date|$offense|$latitude|$longitude|${record.recordNumber}"

        return "NIA-${source.hashCode().toUInt()}"
    }

    private fun parseDateTime(
        value: String
    ): LocalDateTime? {

        if (
            value.isBlank()
        ) {

            return null
        }

        /*
         * First try ISO date/time formats.
         */
        try {

            return OffsetDateTime
                .parse(
                    value
                )
                .toLocalDateTime()

        } catch (
            exception: DateTimeParseException
        ) {

            // Continue.
        }

        try {

            return LocalDateTime
                .parse(
                    value
                )

        } catch (
            exception: DateTimeParseException
        ) {

            // Continue.
        }

        /*
         * Common public-safety / CSV formats.
         */
        val formatters =
            listOf(

                DateTimeFormatter.ofPattern(
                    "yyyy-MM-dd HH:mm:ss"
                ),

                DateTimeFormatter.ofPattern(
                    "yyyy-MM-dd HH:mm"
                ),

                DateTimeFormatter.ofPattern(
                    "MM/dd/yyyy HH:mm:ss"
                ),

                DateTimeFormatter.ofPattern(
                    "MM/dd/yyyy HH:mm"
                ),

                DateTimeFormatter.ofPattern(
                    "M/d/yyyy H:mm:ss"
                ),

                DateTimeFormatter.ofPattern(
                    "M/d/yyyy H:mm"
                )
            )

        formatters
            .forEach {
                    formatter ->

                try {

                    return LocalDateTime
                        .parse(
                            value,
                            formatter
                        )

                } catch (
                    exception: DateTimeParseException
                ) {

                    // Try next formatter.
                }
            }

        return null
    }

    private fun DayOfWeek.toNiaDayNumber():
            Int {

        /*
         * Keep compatibility with the existing Detroit data:
         *
         * Sunday = 1
         * Monday = 2
         * ...
         * Saturday = 7
         */

        return when (
            this
        ) {

            DayOfWeek.SUNDAY ->
                1

            DayOfWeek.MONDAY ->
                2

            DayOfWeek.TUESDAY ->
                3

            DayOfWeek.WEDNESDAY ->
                4

            DayOfWeek.THURSDAY ->
                5

            DayOfWeek.FRIDAY ->
                6

            DayOfWeek.SATURDAY ->
                7
        }
    }
}