package nl.knaw.huc.annorepo.api

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder

@JsonPropertyOrder("@context", "type", "label")

data class ContainerSpecs(
    @JsonProperty("@context") val context: List<String>,
    val type: List<String>,
    val label: String
)