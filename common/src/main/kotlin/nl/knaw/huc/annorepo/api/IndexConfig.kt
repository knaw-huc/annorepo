package nl.knaw.huc.annorepo.api

import java.net.URI

data class IndexConfig(val field: String, val type: IndexType, val url: URI)
