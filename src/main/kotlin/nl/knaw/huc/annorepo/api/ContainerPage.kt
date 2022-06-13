package nl.knaw.huc.annorepo.api

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import nl.knaw.huc.annorepo.api.ARConst.ANNO_JSONLD_URL
import nl.knaw.huc.annorepo.api.ARConst.LDP_JSONLD_URL

@JsonPropertyOrder("@context", "type", "id", "label", "first", "last", "total")
class ContainerPage(
    val id: String,
    val label: String,
    annotations: List<Map<String, Any>>,
    startIndex: Int = 0,
    val total: Long = 0
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
    val first = AnnotationPage(items = annotations, partOf = id, startIndex = startIndex)
    val last = "$id?page=0&desc=1"

}
