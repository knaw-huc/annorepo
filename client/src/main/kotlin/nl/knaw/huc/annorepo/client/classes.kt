package nl.knaw.huc.annorepo.client

import arrow.core.Either
import nl.knaw.huc.annorepo.api.AboutInfo
import nl.knaw.huc.annorepo.api.AnnotationIdentifier
import javax.ws.rs.core.MultivaluedMap
import javax.ws.rs.core.Response

sealed class ARResult {
    abstract val response: Response

    data class GetAboutResult(
        override val response: Response,
        val aboutInfo: AboutInfo
    ) : ARResult()

    data class BatchUploadResult(
        override val response: Response,
        val annotationData: List<AnnotationIdentifier>
    ) : ARResult()

    data class AnnoRepoResult(
        override val response: Response,
        val created: Boolean,
        val location: String,
        val containerId: String,
        val eTag: String
    ) : ARResult()

    data class GetQueryInfoResult(
        override val response: Response,
    ) : ARResult()

    data class GetContainerResult(
        override val response: Response,
    ) : ARResult()

    data class GetContainerMetadataResult(
        override val response: Response,
        val metadata: Map<String, Any>
    ) : ARResult()

    data class AddUsersResult(
        override val response: Response,
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


