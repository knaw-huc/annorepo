package nl.knaw.huc.annorepo.service

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * Converts LocalDateTime result to json datetime format
 *
 *
 * Datetime pattern is configured in config.yml
 */
class LocalDateTimeSerializer(private val dateFormat: String) :
    JsonSerializer<LocalDateTime>() {
    @Throws(IOException::class)
    override fun serialize(
        dateTime: LocalDateTime,
        serializer: JsonGenerator,
        serializerProvider: SerializerProvider
    ) {
        try {
            val result = dateTime.format(DateTimeFormatter.ofPattern(dateFormat))
            serializer.writeString(result)
        } catch (ex: DateTimeParseException) {
            throw RuntimeException(String.format("Could not serialize date [%s]", dateTime), ex)
        }
    }
}