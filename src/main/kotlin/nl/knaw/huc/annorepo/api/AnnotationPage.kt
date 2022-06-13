package nl.knaw.huc.annorepo.api

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder

@JsonPropertyOrder("type", "as:items", "partOf", "startIndex")
class AnnotationPage(
    items: List<Map<String, Any>>,
    val partOf: String,
    val startIndex: Int
) {
    val type: String = "AnnotationPage"

    @JsonProperty("as:items")
    val itemList = mapOf("@list" to items)
}
