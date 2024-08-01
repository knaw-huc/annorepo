package nl.knaw.huc.annorepo.client

import java.net.URI
import java.util.stream.Stream
import jakarta.ws.rs.core.Response
import arrow.core.Either
import nl.knaw.huc.annorepo.api.AnnotationIdentifier
import nl.knaw.huc.annorepo.api.AnnotationPage
import nl.knaw.huc.annorepo.api.ChoreStatusSummary
import nl.knaw.huc.annorepo.api.ContainerUserEntry
import nl.knaw.huc.annorepo.api.IndexConfig
import nl.knaw.huc.annorepo.api.MetadataMap
import nl.knaw.huc.annorepo.api.RejectedUserEntry
import nl.knaw.huc.annorepo.api.SearchInfo
import nl.knaw.huc.annorepo.api.SearchStatusSummary
import nl.knaw.huc.annorepo.api.UserEntry
import nl.knaw.huc.annorepo.api.WebAnnotationAsMap

sealed class ARResult {
    abstract val response: Response

    data class GetAboutResult(
        override val response: Response,
        val aboutInfo: Map<String, Any>,
    ) : ARResult()

    /**
     * Result of a successful call to [AnnoRepoClient#createContainer(String,String)]
     *
     * @property response
     * @property location The URI of the created container.
     * @property containerName The name given to this container.
     * @property eTag The eTag of the container; required when calling [AnnoRepoClient#deleteContainer(String,String)]
     * @constructor Create empty Create container result
     */
    data class CreateContainerResult(
        override val response: Response,
        val location: URI,
        val containerName: String,
        val eTag: String,
    ) : ARResult()

    data class GetContainerResult(
        override val response: Response,
        val entity: String,
        val eTag: String,
    ) : ARResult()

    data class GetContainerMetadataResult(
        override val response: Response,
        val metadata: MetadataMap,
    ) : ARResult()

    data class SetAnonymousUserReadAccessResult(
        override val response: Response
    ) : ARResult()

    data class DeleteResult(
        override val response: Response,
    ) : ARResult()

    data class CreateAnnotationResult(
        override val response: Response,
        val location: URI,
        val containerName: String,
        val annotationName: String,
        val eTag: String,
    ) : ARResult()

    data class GetAnnotationResult(
        override val response: Response,
        val eTag: String,
        val annotation: WebAnnotationAsMap,
    ) : ARResult()

    data class AnnotationFieldInfoResult(
        override val response: Response,
        val fieldInfo: Map<String, Int>,
    ) : ARResult()

    data class DistinctAnnotationFieldValuesResult(
        override val response: Response,
        val distinctValues: List<Any?>,
    ) : ARResult()

    data class BatchUploadResult(
        override val response: Response,
        val annotationData: List<AnnotationIdentifier>,
    ) : ARResult()

    data class CreateSearchResult(
        override val response: Response,
        val location: URI,
        val queryId: String,
    ) : ARResult()

    data class GetSearchInfoResult(
        override val response: Response,
        val searchInfo: SearchInfo,
    ) : ARResult()

    data class GetSearchResultPageResult(
        override val response: Response,
        val annotationPage: AnnotationPage,
    ) : ARResult()

    data class GetGlobalSearchStatusResult(
        override val response: Response,
        val searchStatus: SearchStatusSummary,
    ) : ARResult()

    data class AddIndexResult(
        override val response: Response,
        val status: ChoreStatusSummary,
    ) : ARResult()

    data class GetIndexResult(
        override val response: Response,
        val indexConfig: IndexConfig,
    ) : ARResult()

    data class GetIndexCreationStatusResult(
        override val response: Response,
        val status: ChoreStatusSummary,
    ) : ARResult()

    data class ListIndexesResult(
        override val response: Response,
        val indexes: List<IndexConfig>,
    ) : ARResult()

    data class AddUsersResult(
        override val response: Response,
        val accepted: List<String>,
        val rejected: List<RejectedUserEntry>,
    ) : ARResult()

    data class UsersResult(
        override val response: Response,
        val userEntries: List<UserEntry>,
    ) : ARResult()

    data class ContainerUsersResult(
        override val response: Response,
        val containerUserEntries: List<ContainerUserEntry>,
    ) : ARResult()

    data class MyContainersResult(
        override val response: Response,
        val containers: Map<String, List<String>>
    ) : ARResult()

    data class FilterContainerAnnotationsResult(
        override val response: Response,
        val queryId: String,
        val annotations: Stream<Either<RequestError, String>>,
    ) : ARResult()

    data class CreateCustomQueryResult(
        override val response: Response,
        val location: URI?
    ) : ARResult()

}

sealed class RequestError {
    abstract val message: String

    data class NotAuthorized(
        override val message: String,
        val response: Response,
        val responseString: String,
    ) : RequestError()

    data class UnexpectedResponse(
        override val message: String,
        val response: Response,
        val responseString: String,
    ) : RequestError()

    data class ConnectionError(
        override val message: String,
    ) : RequestError()
}

typealias ResponseHandlerMap<T> = Map<Response.Status, (Response) -> Either<RequestError, T>>

data class CustomQuery(
    val name: String,
    val queryTemplate: Map<String, Any>,
    val label: String = "",
    val description: String = "",
    val public: Boolean = true
)
