package nl.knaw.huc.annorepo.api

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder

@JsonPropertyOrder("@context", "id", "type", "total", "label", "modified", "first", "last")
@JsonInclude(JsonInclude.Include.NON_NULL)
class ContainerPage(
    val id: String,
    val label: String,
    annotations: List<WebAnnotationAsMap>,
    page: Int = 0,
    val total: Long = 0,
    lastPage: Int,
    prevPage: Int? = null,
    nextPage: Int? = null
) {
    @JsonProperty("@context")
    val context = listOf(
        ANNO_JSONLD_URL,
        LDP_JSONLD_URL
    )
    val type = listOf(
        "BasicContainer",
        "AnnotationCollection"
    )
    val last = "$id?page=$lastPage"
    val first = AnnotationPage(
        id = "$id?page=$page",
        partOf = id,
        startIndex = page,
        items = annotations,
        prev = if (prevPage != null) "$id?page=$prevPage" else null,
        next = if (nextPage != null) "$id?page=$nextPage" else null
    )
}
