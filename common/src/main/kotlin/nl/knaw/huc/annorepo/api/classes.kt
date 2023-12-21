package nl.knaw.huc.annorepo.api

import java.net.URI
import java.util.Date

typealias WebAnnotationAsMap = Map<String, Any>
typealias QueryAsMap = Map<String, Any>
typealias MetadataMap = Map<String, Any>

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
    val mongoVersion: String,
//    val grpcHostName: String,
//    val grpcPort: Int
)

data class AnnotationIdentifier(
    val annotationName: String,
    val containerName: String,
    val etag: String, // don't change this to eTag!
)

data class IndexConfig(
    val field: String,
    val type: IndexType,
    val url: URI
)

data class SearchInfo(
    val query: QueryAsMap,
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
    val expiresAfter: Date?,
    val state: String,
    val containersSearched: Int,
    val totalContainersToSearch: Int,
    val hitsFoundSoFar: Int,
    val errors: List<String>,
    val processingTimeInMillis: Long
)

data class ChoreStatusSummary(
    val startedAt: Date,
    val finishedAt: Date?,
    val expiresAfter: Date?,
    val state: String,
    val errors: List<String>,
    val processingTimeInMillis: Long
)
