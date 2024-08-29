package nl.knaw.huc.annorepo.dao

import java.time.Instant
import java.util.Date
import com.fasterxml.jackson.annotation.JsonFormat

data class CustomQuery(
    val name: String,
    val description: String? = null,
    val label: String? = null,

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssZ")
    val created: Date = Date.from(Instant.now()),

    val createdBy: String = "",
    val public: Boolean = true,
    val queryTemplate: String,
    val parameters: List<String> = emptyList()
)
