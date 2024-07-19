package nl.knaw.huc.annorepo.resources.tools

import kotlin.Result.Companion.failure
import kotlin.Result.Companion.success
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.commons.text.StringEscapeUtils
import org.apache.jena.ext.xerces.impl.dv.util.Base64
import nl.knaw.huc.annorepo.api.isValidContainerName

object CustomQueryTools {

    data class CustomQueryCall(
        val name: String,
        val parameters: Map<String, String>
    )

    class BadQueryCall(message: String) : Throwable(message)

    fun decode(customQueryCallString: String): Result<CustomQueryCall> {
        val parts = customQueryCallString.split(":")
        val name = parts[0]
        return if (parts.size <= 1) {
            success(CustomQueryCall(name, emptyMap()))
        } else {
            val errors = mutableListOf<String>()
            val encodedParams = parts[1].split(',')
            val parameters: Map<String, String> = encodedParams.associate { ep ->
                val (parName, encodedValue) = ep.split("=", limit = 2)
                val decoded = Base64.decode(encodedValue)
                if (decoded == null) {
                    errors.add("bad Base64 value '$encodedValue' for parameter $parName")
                }
                val value = decoded?.decodeToString() ?: ""
                (parName to value)
            }
            if (errors.isNotEmpty()) {
                failure(BadQueryCall(errors.joinToString(", ")))
            } else {
                success(CustomQueryCall(name, parameters))
            }
        }
    }

    fun encode(customQueryCall: CustomQueryCall): String =
        if (customQueryCall.parameters.isEmpty()) {
            customQueryCall.name
        } else {
            val encodedParams = customQueryCall.parameters
                .map { (k, v) ->
                    val encodedValue = Base64.encode(v.encodeToByteArray())
                    "$k=$encodedValue"
                }
                .joinToString(",")
            "${customQueryCall.name}:$encodedParams"
        }

    fun String.isValidQueryName(): Boolean = isValidContainerName()
    fun String.isValidParameterName(): Boolean = isValidContainerName()

    fun String.isValidQueryTemplate(): Boolean {
        try {
            ObjectMapper().readValue(this, HashMap::class.java)
        } catch (e: Exception) {
            return false
        }
        return true
    }

    private const val PATTERN = "<([A-Za-z0-9_-]+)>"
    fun String.extractParameterNames(): List<String> =
        Regex(PATTERN)
            .findAll(this)
            .mapNotNull { it.groups[1]?.value }
            .toList()

    fun String.interpolate(queryParameters: Map<String, String>): String =
        queryParameters.entries.fold(this) { expanded, (k, v) ->
            expanded.replace("<$k>", StringEscapeUtils.escapeJson(v))
        }

}

