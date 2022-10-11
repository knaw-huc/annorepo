package nl.knaw.huc.annorepo.client

import arrow.core.Either
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import nl.knaw.huc.annorepo.api.AboutInfo
import nl.knaw.huc.annorepo.api.AnnotationIdentifier
import nl.knaw.huc.annorepo.api.IndexType
import nl.knaw.huc.annorepo.api.ResourcePaths.ABOUT
import nl.knaw.huc.annorepo.api.ResourcePaths.ADMIN
import nl.knaw.huc.annorepo.api.ResourcePaths.BATCH
import nl.knaw.huc.annorepo.api.ResourcePaths.FIELDS
import nl.knaw.huc.annorepo.api.ResourcePaths.SERVICES
import nl.knaw.huc.annorepo.api.ResourcePaths.USERS
import nl.knaw.huc.annorepo.api.ResourcePaths.W3C
import nl.knaw.huc.annorepo.api.UserEntry
import nl.knaw.huc.annorepo.client.ARResponse.AnnoRepoResponse
import nl.knaw.huc.annorepo.client.ARResponse.BatchUploadResponse
import nl.knaw.huc.annorepo.client.RequestError.ConnectionError
import nl.knaw.huc.annorepo.util.extractVersion
import org.glassfish.jersey.client.filter.EncodingFilter
import org.glassfish.jersey.message.GZipEncoder
import org.slf4j.LoggerFactory
import java.net.URI
import javax.ws.rs.client.ClientBuilder
import javax.ws.rs.client.Entity
import javax.ws.rs.client.Invocation
import javax.ws.rs.client.WebTarget
import javax.ws.rs.core.Response

typealias ResponseHandlerMap<T> = Map<Response.Status, (Response) -> Either<RequestError, T>>

class AnnoRepoClient(serverURI: URI, val apiKey: String? = null, private val userAgent: String? = null) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val webTarget: WebTarget =
        ClientBuilder.newClient()
            .apply {
                register(GZipEncoder::class.java)
                register(EncodingFilter::class.java)
            }
            .target(serverURI)
    private val oMapper: ObjectMapper = ObjectMapper().registerKotlinModule()

    var serverVersion: String? = null
    var serverNeedsAuthentication: Boolean? = null

    init {
        log.info("checking annorepo server at $serverURI ...")
        getAbout().bimap(
            { e ->
                log.error("error: {}", e)
                throw RuntimeException("Unable to connect to annorepo server")
            },
            { aboutInfo ->
                serverVersion = aboutInfo.version
                serverNeedsAuthentication = aboutInfo.withAuthentication
                log.info("$serverURI runs version $serverVersion ; needs authentication: $serverNeedsAuthentication")
            }
        )
    }

    fun getAbout(): Either<RequestError, AboutInfo> =
        doGet(
            request = webTarget.path(ABOUT)
                .request(),
            responseHandlers = mapOf(
                Response.Status.OK to { response: Response ->
                    val json = response.readEntity(String::class.java);
                    Either.Right(oMapper.readValue(json))
                }
            )
        )

    fun createContainer(
        preferredName: String? = null,
        label: String = "A container for web annotations"
    ): Either<RequestError, AnnoRepoResponse> {
        var request = webTarget.path(W3C).request()
        if (preferredName != null) {
            request = request.header("slug", preferredName)
        }
        return doPost(
            request = request,
            entity = Entity.json(containerSpecs(label)),
            responseHandlers = mapOf(
                Response.Status.CREATED to { response ->
                    val location = location(response) ?: ""
                    val containerId = extractContainerName(location)
                    val eTag = eTag(response) ?: ""
                    Either.Right(
                        AnnoRepoResponse(
                            created = true,
                            location = location,
                            containerId = containerId,
                            eTag = eTag
                        )
                    )
                }
            )
        )
    }

    fun deleteContainer(containerName: String, eTag: String): Either<RequestError, Boolean> =
        doDelete(
            request = webTarget.path(W3C).path(containerName)
                .request()
                .header("if-match", eTag),
            responseHandlers = mapOf(
                Response.Status.NO_CONTENT to { Either.Right(true) }
            )
        )

    fun createAnnotation(containerName: String, annotation: Map<String, Any>): Either<RequestError, AnnoRepoResponse> =
        doPost(
            request = webTarget.path(W3C).path(containerName).request(),
            entity = Entity.json(annotation),
            responseHandlers = mapOf(
                Response.Status.CREATED to { response ->
                    val location = location(response) ?: ""
                    val annotationName = extractAnnotationName(location)
                    val eTag = eTag(response) ?: ""
                    Either.Right(
                        AnnoRepoResponse(true, location, containerId = annotationName, eTag = eTag)
                    )
                }
            )
        )

    fun updateAnnotation(
        containerName: String,
        annotationName: String,
        eTag: String,
        annotation: Map<String, Any>
    ): Either<RequestError, AnnoRepoResponse> =
        doPut(
            request = webTarget.path(W3C).path(containerName).path(annotationName)
                .request()
                .header("if-match", eTag),
            entity = Entity.json(annotation),
            responseHandlers = mapOf(
                Response.Status.NO_CONTENT to { response ->
                    val location = location(response) ?: ""
                    val newEtag = eTag(response) ?: ""
                    Either.Right(
                        AnnoRepoResponse(false, location, containerId = annotationName, eTag = newEtag)
                    )
                }
            )
        )

    fun deleteAnnotation(containerName: String, annotationName: String, eTag: String): Either<RequestError, Boolean> =
        doDelete(
            request = webTarget.path(W3C).path(containerName).path(annotationName)
                .request()
                .header("if-match", eTag),
            responseHandlers = mapOf(
                Response.Status.NO_CONTENT to { Either.Right(true) }
            )
        )

    fun getFieldCount(containerName: String): Either<RequestError, Map<String, Int>> =
        doGet(
            request = webTarget.path(SERVICES).path(containerName).path(FIELDS)
                .request(),
            responseHandlers = mapOf(
                Response.Status.OK to { response ->
                    val json = response.readEntity(String::class.java)
                    Either.Right(oMapper.readValue(json))
                }
            )
        )

    fun batchUpload(
        containerName: String,
        annotations: List<Map<String, Any>>
    ): Either<RequestError, BatchUploadResponse> =
        doPost(
            request = webTarget.path(BATCH).path(containerName).path("annotations").request(),
            entity = Entity.json(annotations),
            responseHandlers = mapOf(
                Response.Status.OK to { response ->
                    val entityJson: String =
                        response.readEntity(String::class.java)
                    val annotationData: List<AnnotationIdentifier> = oMapper.readValue(entityJson)
                    Either.Right(BatchUploadResponse(annotationData))
                }
            )
        )

    fun createQuery(containerName: String, query: Map<String, Any>): Either<RequestError, String> =
        doPost(
            request = webTarget.path(SERVICES).path(containerName).path("search")
                .request(),
            entity = Entity.json(query),
            responseHandlers = mapOf(
                Response.Status.CREATED to { response ->
                    val location = response.location
                    Either.Right(location.rawPath.split("/").last())
                }
            )
        )

    fun getQueryResult(containerName: String, queryId: String, page: Int): Either<RequestError, String> =
        doGet(
            request = webTarget.path(SERVICES).path(containerName).path("search").path(queryId)
                .queryParam("page", page)
                .request(),
            responseHandlers = mapOf(
                Response.Status.OK to { response ->
                    Either.Right(response.readEntity(String::class.java))
                }
            )
        )

    fun addIndex(containerName: String, fieldName: String, indexType: IndexType): Either<RequestError, Response> =
        doPut(
            request = webTarget.path(SERVICES).path(containerName).path("indexes").path(fieldName).path(indexType.name)
                .request(),
            entity = Entity.json(emptyMap<String, Any>()),
            responseHandlers = mapOf(
                Response.Status.CREATED to { response -> Either.Right(response) }
            )
        )

    fun listIndexes(containerName: String): Either<RequestError, String> =
        doGet(
            request = webTarget.path(SERVICES).path(containerName).path("indexes")
                .request(),
            responseHandlers = mapOf(
                Response.Status.OK to { response ->
                    Either.Right(response.readEntity(String::class.java))
                }
            )
        )

    fun deleteIndex(containerName: String, fieldName: String, indexType: IndexType): Either<RequestError, Boolean> =
        doDelete(
            request = webTarget.path(SERVICES).path(containerName).path("indexes").path(fieldName).path(indexType.name)
                .request(),
            responseHandlers = mapOf(
                Response.Status.NO_CONTENT to { Either.Right(true) }
            )
        )

    fun getUsers(): Either<RequestError, List<UserEntry>> =
        doGet(
            request = webTarget.path(ADMIN).path(USERS)
                .request(),
            responseHandlers = mapOf(
                Response.Status.OK to { response ->
                    val json = response.readEntity(String::class.java)
                    val userEntryList = oMapper.readValue(json, object : TypeReference<List<UserEntry>>() {})
                    Either.Right(userEntryList)
                }
            )
        )

    // private functions
    private fun <T> doGet(
        request: Invocation.Builder,
        responseHandlers: ResponseHandlerMap<T>
    ): Either<RequestError, T> =
        doRequest {
            request
                .withHeaders()
                .get()
                .processResponseWith(responseHandlers)
        }

    private fun <T> doPost(
        request: Invocation.Builder,
        entity: Entity<*>,
        responseHandlers: ResponseHandlerMap<T>
    ): Either<RequestError, T> =
        doRequest {
            request
                .withHeaders()
                .post(entity)
                .processResponseWith(responseHandlers)
        }

    private fun <T> doPut(
        request: Invocation.Builder,
        entity: Entity<*>,
        responseHandlers: ResponseHandlerMap<T>
    ): Either<RequestError, T> =
        doRequest {
            request
                .withHeaders()
                .put(entity)
                .processResponseWith(responseHandlers)
        }

    private fun <T> doDelete(
        request: Invocation.Builder,
        responseHandlers: ResponseHandlerMap<T>
    ): Either<RequestError, T> =
        doRequest {
            request
                .withHeaders()
                .delete()
                .processResponseWith(responseHandlers)
        }

    private fun <T> Response.processResponseWith(
        responseHandlers: Map<Response.Status, (Response) -> Either<RequestError, T>>
    ): Either<RequestError, T> {
        val handlerIdx = responseHandlers.entries.associate { it.key.statusCode to it.value }
        return when (status) {
            in handlerIdx.keys -> handlerIdx[status]!!.invoke(this)
            Response.Status.UNAUTHORIZED.statusCode -> unauthorizedResponse(this)
            else -> unexpectedResponse(this)
        }
    }

    private fun unauthorizedResponse(response: Response): Either.Left<RequestError> =
        Either.Left(
            RequestError.NotAuthorized(
                message = "Not authorized to make this call; check your apiKey",
                headers = response.headers,
                responseString = response.readEntity(String::class.java)
            )
        )

    private fun unexpectedResponse(response: Response): Either.Left<RequestError> =
        Either.Left(
            RequestError.UnexpectedResponse(
                message = "Unexpected status: ${response.status}",
                headers = response.headers,
                responseString = response.readEntity(String::class.java)
            )
        )

    private fun extractContainerName(location: String): String {
        val parts = location.split("/")
        return parts[parts.size - 2]
    }

    private fun extractAnnotationName(location: String): String {
        val parts = location.split("/")
        return parts[parts.size - 1]
    }

    private fun location(response: Response): String? =
        response.firstHeader("location")

    private fun eTag(response: Response): String? =
        response.firstHeader("etag")

    private fun <R> doRequest(requestHandler: () -> Either<RequestError, R>): Either<RequestError, R> =
        try {
            requestHandler()
        } catch (e: Exception) {
            Either.Left(ConnectionError(e.message ?: "Connection Error"))
        }

    private fun Response.firstHeader(key: String): String? =
        if (headers.containsKey(key)) {
            val locations: MutableList<Any> = headers[key]!!
            locations[0].toString()
        } else {
            null
        }

    private fun Invocation.Builder.withHeaders(): Invocation.Builder {
        val libUA = "${AnnoRepoClient::class.java.name}/${getVersion() ?: ""}"
        val ua = if (userAgent == null) {
            libUA
        } else {
            "$userAgent ( using $libUA )"
        }
        var builder = header("User-Agent", ua)
            .header("Accept-Encoding", "gzip")
            .header("Content-Encoding", "gzip")

        if (serverNeedsAuthentication != null && serverNeedsAuthentication!!) {
            builder = builder.header("Authorization", "Bearer $apiKey")
        }
        return builder
    }

    private fun containerSpecs(label: String) = mapOf(
        "@context" to listOf(
            "http://www.w3.org/ns/anno.jsonld",
            "http://www.w3.org/ns/ldp.jsonld"
        ),
        "type" to listOf(
            "BasicContainer",
            "AnnotationCollection"
        ),
        "label" to label
    )

    private fun getVersion(): String? =
        this.javaClass.extractVersion()

}