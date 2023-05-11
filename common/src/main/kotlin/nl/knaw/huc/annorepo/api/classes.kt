package nl.knaw.huc.annorepo.api

import java.net.URI
import java.util.Date

data class RejectedUserEntry(
    val userEntry: Map<String, String>,
    val reason: String,
)

data class UserAddResults(
    val added: List<String>,
    val rejected: List<RejectedUserEntry>,
)

data class AboutInfo(
    val appName: String,
    val version: String,
    val startedAt: String,
    val baseURI: String,
    val withAuthentication: Boolean,
    val sourceCode: String = "https://github.com/knaw-huc/annorepo",
)

data class AnnotationIdentifier(
    val annotationName: String,
    val containerName: String,
    val eTag: String,
)

data class IndexConfig(
    val field: String,
    val type: IndexType,
    val url: URI,
)

data class SearchInfo(
    val query: Map<String, Any>,
    val hits: Int,
)

data class UserEntry(
    val userName: String,
    val apiKey: String,
)

data class UserAccessEntry(
    val userName: String,
    val containerName: String,
    val role: Role,
)

data class ContainerUserEntry(
    val userName: String,
    val role: Role,
)

class MissingTargetException : Exception("WebAnnotation must have 1 or more targets")
data class SearchStatusSummary(
    val query: HashMap<*, *>,
    val startedAt: Date,
    val finishedAt: Date?,
    val expiresAt: Date?,
    val state: String,
    val containersSearched: Int,
    val totalContainersToSearch: Int,
    val hitsFoundSoFar: Int,
    val errors: List<String>,
    val processingTimeInMillis: Long
)
