package nl.knaw.huc.annorepo.resources.tools

import kotlin.Result.Companion.failure
import kotlin.Result.Companion.success
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
        return if (parts.size > 1) {
            val errors = mutableListOf<String>()
            val encodedParams = parts[1].split(',')
            val parameters: Map<String, String> = encodedParams.associate { ep ->
                val (parName, encodedValue) = ep.split("=")
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
        } else {
            success(CustomQueryCall(name, emptyMap()))
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

    fun expandQueryTemplate(queryTemplate: String, queryParameters: Map<String, String>): String {
        var expanded = queryTemplate
        queryParameters.forEach { (k, v) ->
            expanded = expanded.replace("<$k>", StringEscapeUtils.escapeJson(v))
        }
        return expanded
    }

}

