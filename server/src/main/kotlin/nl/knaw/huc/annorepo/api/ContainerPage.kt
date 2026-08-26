package nl.knaw.huc.annorepo.api

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import nl.knaw.huc.annorepo.resources.tools.annotationCollectionLink

@JsonPropertyOrder("@context", "id", "type", "total", "label", "modified", "first", "last")
@JsonInclude(JsonInclude.Include.NON_NULL)
class ContainerPage(
    val id: String,
    val label: String,
    annotations: List<WebAnnotationAsMap>,
    page: Int = 0,
    val total: Long = 0,
    prevPage: Int? = null,
    nextPage: Int? = null
) {
    @JsonProperty("@context")
    val context = listOf(
        ANNO_JSONLD_URL,
        LDP_JSONLD_URL
    ) + annotations.uniqueCustomContextElements()

    private fun List<WebAnnotationAsMap>.uniqueCustomContextElements(): Set<Any> =
        mapNotNull { it["@context"] }
            .flatMap { it.contextElements() }
            .filterNot { it == ANNO_JSONLD_URL || it == LDP_JSONLD_URL }
            .toSet()

    private fun Any.contextElements(): List<Any> =
        when (this) {
            is List<*> -> this.filterNotNull()
            else -> listOf(this)
        }

    val type = listOf(
        "BasicContainer",
        "AnnotationCollection"
    )

    //    val last = "$id?page=$lastPage"
    val first = AnnotationPage(
        id = "$id?page=$page",
        partOf = annotationCollectionLink(id),
        startIndex = page,
        items = annotations.map { it.withoutContext() },
        prev = if (prevPage != null) "$id?page=$prevPage" else null,
        next = if (nextPage != null) "$id?page=$nextPage" else null
    )

    private fun WebAnnotationAsMap.withoutContext(): Map<String, Any> =
        toMutableMap()
            .apply { remove("@context") }
}
