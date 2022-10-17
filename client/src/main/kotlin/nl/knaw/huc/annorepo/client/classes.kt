package nl.knaw.huc.annorepo.client

import arrow.core.Either
import nl.knaw.huc.annorepo.api.AboutInfo
import nl.knaw.huc.annorepo.api.AnnotationIdentifier
import nl.knaw.huc.annorepo.api.UserEntry
import java.net.URI
import javax.ws.rs.core.MultivaluedMap
import javax.ws.rs.core.Response

sealed class ARResult {
    abstract val response: Response

    data class GetAboutResult(
        override val response: Response,
        val aboutInfo: AboutInfo
    ) : ARResult()

    data class CreateContainerResult(
        override val response: Response,
        val location: URI,
        val containerName: String,
        val eTag: String
    ) : ARResult()

    data class GetContainerResult(
        override val response: Response,
        val eTag: String
    ) : ARResult()

    data class GetContainerMetadataResult(
        override val response: Response,
        val metadata: Map<String, Any>
    ) : ARResult()

    data class DeleteContainerResult(
        override val response: Response,
    ) : ARResult()

    data class CreateAnnotationResult(
        override val response: Response,
        val location: URI,
        val containerName: String,
        val annotationName: String,
        val eTag: String
    ) : ARResult()

    data class GetAnnotationResult(
        override val response: Response,
        val eTag: String,
        val annotation: Map<String, Any>
    ) : ARResult()

    data class DeleteAnnotationResult(
        override val response: Response,
    ) : ARResult()

    data class AnnotationFieldInfoResult(
        override val response: Response,
        val fieldInfo: Map<String, Int>
    ) : ARResult()

    data class BatchUploadResult(
        override val response: Response,
        val annotationData: List<AnnotationIdentifier>
    ) : ARResult()

    data class CreateQueryResult(
        override val response: Response,
        val location: URI,
        val queryId: String
    ) : ARResult()

    data class GetQueryInfoResult(
        override val response: Response,
    ) : ARResult()

    data class QueryResultPageResult(
        override val response: Response,
    ) : ARResult()

    data class AddIndexResult(
        override val response: Response,
    ) : ARResult()

    data class ListIndexesResult(
        override val response: Response,
        val indexes: List<Map<String, Any>>
    ) : ARResult()

    data class DeleteIndexResult(
        override val response: Response,
    ) : ARResult()

    data class AddUsersResult(
        override val response: Response,
    ) : ARResult()

    data class UsersResult(
        override val response: Response,
        val userEntries: List<UserEntry>,
    ) : ARResult()

    data class DeleteUserResult(
        override val response: Response,
    ) : ARResult()

}

sealed class RequestError {
    abstract val message: String

    data class NotAuthorized(
        override val message: String,
        val headers: MultivaluedMap<String, Any>,
        val responseString: String
    ) : RequestError()

    data class UnexpectedResponse(
        override val message: String,
        val headers: MultivaluedMap<String, Any>,
        val responseString: String
    ) : RequestError()

    data class ConnectionError(
        override val message: String
    ) : RequestError()
}

typealias ResponseHandlerMap<T> = Map<Response.Status, (Response) -> Either<RequestError, T>>


