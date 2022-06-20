package nl.knaw.huc.annorepo.api

import com.fasterxml.jackson.annotation.JsonPropertyOrder

@JsonPropertyOrder("type", "items", "partOf", "startIndex")
class AnnotationPage(
    val items: List<Map<String, Any>>,
    val partOf: String,
    val startIndex: Int
) {
    val type: String = "AnnotationPage"
}
