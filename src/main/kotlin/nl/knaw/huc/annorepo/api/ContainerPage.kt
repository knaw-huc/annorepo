package nl.knaw.huc.annorepo.api

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import nl.knaw.huc.annorepo.api.ARConst.ANNO_JSONLD_URL
import nl.knaw.huc.annorepo.api.ARConst.LDP_JSONLD_URL

@JsonPropertyOrder("@context", "type", "id", "label", "first", "prev", "next", "last", "total")
@JsonInclude(JsonInclude.Include.NON_NULL)
class ContainerPage(
    val id: String,
    val label: String,
    annotations: List<Map<String, Any>>,
    page: Int = 0,
    val total: Long = 0,
    prevPage: Int?,
    nextPage: Int?,
    lastPage: Int
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
    val first = AnnotationPage(items = annotations, partOf = id, startIndex = page)
    val prev = if (prevPage != null) "$id?page=$prevPage" else null
    val next = if (nextPage != null) "$id?page=$nextPage" else null
    val last = "$id?page=$lastPage"
}
