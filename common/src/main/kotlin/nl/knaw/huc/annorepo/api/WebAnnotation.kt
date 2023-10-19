package nl.knaw.huc.annorepo.api

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder

@JsonPropertyOrder("@context", "type", "body", "target")
@JsonInclude(NON_NULL)
class WebAnnotation private constructor(
    val body: Any?,
    val target: Any?,
) {
    @JsonProperty("@context")
    val context = ANNO_JSONLD_URL
    val type = "Annotation"

    class Builder {
        var body: Any? = null
        var target: Any? = null

        fun withBody(body: Any) = apply { this.body = body }
        fun withTarget(target: Any) = apply { this.target = target }

        fun build(): WebAnnotation {
            if (target == null) {
                throw MissingTargetException()
            }
            return WebAnnotation(body, target)
        }
    }

    fun asMap(): WebAnnotationAsMap {
        return mapOf(
            "@context" to ANNO_JSONLD_URL,
            "type" to "Annotation",
            "body" to body!!,
            "target" to target!!
        )
    }

    companion object {
        @JvmStatic
        fun template(): MutableMap<String, Any> =
            mutableMapOf(
                "@context" to ANNO_JSONLD_URL,
                "type" to "Annotation"
            )
    }
}