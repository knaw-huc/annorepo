package nl.knaw.huc.annorepo.resources.tools

import jakarta.ws.rs.BadRequestException
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.apache.logging.log4j.kotlin.logger
import org.bson.Document
import nl.knaw.huc.annorepo.api.ANNO_JSONLD_URL
import nl.knaw.huc.annorepo.api.WebAnnotationAsMap

private const val W3ORG_URL = "https://www.w3.org/TR/annotation-model/#annotations"

private const val INVALID_INPUT = "Invalid input"

private const val MISSING_CONTEXT = "The Annotation MUST have 1 or more @context values."
private const val MISSING_REQUIRED_CONTEXT =
    "The Annotation MUST have 1 or more @context values and $ANNO_JSONLD_URL MUST be one of them."
private const val INVALID_CONTEXT = "invalid @context."

private const val MISSING_TYPE = "An Annotation MUST have 1 or more types."
private const val MISSING_REQUIRED_TYPE =
    "An Annotation MUST have 1 or more types, and the Annotation class MUST be one of them."

private const val MISSING_TARGET =
    "There MUST be 1 or more target relationships associated with an Annotation."

// see https://www.w3.org/TR/annotation-model/#annotations
fun Document.validate() {
    val errors = mutableListOf<String>()
    val context = this["@context"]
    errors.addIfNotNull(validateContext(context))
    val type = this["type"]
    errors.addIfNotNull(validateType(type))
    val target = this["target"]
    errors.addIfNotNull(validateTarget(target))
    if (errors.isNotEmpty()) {
        throw BadRequestException("$INVALID_INPUT:\n${errors.joinToString("\n")}\nsee $W3ORG_URL")
    }
}

fun WebAnnotationAsMap.validate() {
    logger.info { this }
    val errors = mutableListOf<String>()
    val context = this["@context"]
    errors.addIfNotNull(validateContext(context))
    val type = this["type"]
    errors.addIfNotNull(validateType(type))
    val target = this["target"]
    errors.addIfNotNull(validateTarget(target))
    if (errors.isNotEmpty()) {
        val json = jacksonObjectMapper().writeValueAsString(this)
        val message = """$INVALID_INPUT:
            |${errors.joinToString("\n")}
            |see $W3ORG_URL
            |Annotation: ${json}""".trimMargin()
        throw BadRequestException(message)
    }
}

private fun validateContext(context: Any?): String? =
    when (context) {
        null -> MISSING_CONTEXT
        ANNO_JSONLD_URL -> null
        is String -> MISSING_REQUIRED_CONTEXT
        is List<*> if ANNO_JSONLD_URL in context -> null
        is List<*> -> MISSING_REQUIRED_CONTEXT
        else -> INVALID_CONTEXT
    }

private fun validateType(type: Any?): String? =
    when (type) {
        null -> MISSING_TYPE
        "Annotation" -> null
        is List<*> if "Annotation" in type -> null
        else -> MISSING_REQUIRED_TYPE
    }

private fun validateTarget(target: Any?): String? =
    when (target) {
        null -> MISSING_TARGET
        else -> null
    }

private fun MutableList<String>.addIfNotNull(validationError: String?) =
    validationError?.let { add(it) }
