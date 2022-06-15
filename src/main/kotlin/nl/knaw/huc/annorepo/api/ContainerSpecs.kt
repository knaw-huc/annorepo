package nl.knaw.huc.annorepo.api

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder

@JsonPropertyOrder("@context", "type", "label")

data class ContainerSpecs(
    // context has to be a var to prevent read_only=true in swagger
    @JsonProperty("@context") var context: List<String>,
    val type: List<String>,
    val label: String
)