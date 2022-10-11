package nl.knaw.huc.annorepo.client

import nl.knaw.huc.annorepo.api.AboutInfo
import nl.knaw.huc.annorepo.api.AnnotationIdentifier
import javax.ws.rs.core.MultivaluedMap

sealed class ARResponse {
    data class AboutResponse(val aboutInfo: AboutInfo? = null) : ARResponse()
    data class BatchUploadResponse(val annotationData: List<AnnotationIdentifier>) : ARResponse()
    data class AnnoRepoResponse(val created: Boolean, val location: String, val containerId: String, val eTag: String)

}

sealed class RequestError(val errorMessage: String) {
    data class NotAuthorized(
        val message: String,
        val headers: MultivaluedMap<String, Any>,
        val responseString: String
    ) :
        RequestError(message)

    data class UnexpectedResponse(
        val message: String,
        val headers: MultivaluedMap<String, Any>,
        val responseString: String
    ) :
        RequestError(message)

    data class ConnectionError(val message: String) : RequestError(message)
}


