package nl.knaw.huc.annorepo.api

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder

@JsonPropertyOrder("@context", "type", "label", "readOnlyForAnonymousUsers")

data class ContainerSpecs(
    // context has to be a var to prevent read_only=true in swagger
    @JsonProperty("@context") val context: List<String>,
    val type: List<String>,
    val label: String,
    val readOnlyForAnonymousUsers: Boolean
)