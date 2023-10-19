package nl.knaw.huc.annorepo.api

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder

@JsonPropertyOrder("@context", "id", "type", "partOf", "startIndex", "prev", "next", "items")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class AnnotationPage(
    val id: String,
    val partOf: String,
    val startIndex: Int,
    val items: List<WebAnnotationAsMap>,
    @JsonProperty("@context") val context: List<String>? = null,
    val prev: String? = null,
    val next: String? = null
) {
    val type: String = "AnnotationPage"
}
