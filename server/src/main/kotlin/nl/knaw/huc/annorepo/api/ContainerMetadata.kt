package nl.knaw.huc.annorepo.api

import java.time.Instant

data class ContainerMetadata(
    val name: String,
    val label: String,
    val createdAt: Instant = Instant.now(),
    val modifiedAt: Instant = Instant.now(),
    val fieldCounts: Map<String, Int> = mapOf()
)