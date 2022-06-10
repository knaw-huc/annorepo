package nl.knaw.huc.annorepo.resources

import com.fasterxml.jackson.annotation.JsonProperty

data class ContainerSpecs(
    @JsonProperty("@context") val context: List<String>,
    val type: List<String>,
    val label: String
)