package nl.knaw.huc.annorepo.api

import java.time.Instant

data class ContainerMetadata(val name: String, val label: String, val createdAt: Instant = Instant.now())