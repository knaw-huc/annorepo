package nl.knaw.huc.annorepo.client

import arrow.core.Either
import nl.knaw.huc.annorepo.api.AboutInfo
import nl.knaw.huc.annorepo.api.AnnotationIdentifier
import javax.ws.rs.core.MultivaluedMap
import javax.ws.rs.core.Response

sealed class ARResponse(val response: Response) {
    data class AboutResponse(val r: Response, val aboutInfo: AboutInfo? = null) : ARResponse(r)
    data class BatchUploadResponse(val r: Response, val annotationData: List<AnnotationIdentifier>) : ARResponse(r)
    data class AnnoRepoResponse(
        val r: Response,
        val created: Boolean,
        val location: String,
        val containerId: String,
        val eTag: String
    ) : ARResponse(r)

}

sealed class RequestError(val errorMessage: String) {
    data class NotAuthorized(
        val message: String,
        val headers: MultivaluedMap<String, Any>,
        val responseString: String
    ) : RequestError(message)

    data class UnexpectedResponse(
        val message: String,
        val headers: MultivaluedMap<String, Any>,
        val responseString: String
    ) : RequestError(message)

    data class ConnectionError(val message: String) : RequestError(message)
}

typealias ResponseHandlerMap<T> = Map<Response.Status, (Response) -> Either<RequestError, T>>


