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
import org.eclipse.jetty.http.HttpStatus
import org.eclipse.jetty.http.HttpStatus.NO_CONTENT_204
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
        doRequest {
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
        }

    fun createContainer(
        preferredName: String? = null,
        label: String = "A container for web annotations"
    ): Either<RequestError, AnnoRepoResponse> =
        doRequest {
            var request = webTarget.path(W3C).request()
            if (preferredName != null) {
                request = request.header("slug", preferredName)
            }
            doPost(
                request = request,
                entity = Entity.json(containerSpecs(label)),
                responseHandlers = mapOf(
                    Response.Status.CREATED to { response ->
                        val location = location(response) ?: ""
                        val containerId = extractContainerName(location)
                        val etag = eTag(response) ?: ""
                        Either.Right(
                            AnnoRepoResponse(
                                created = true,
                                location = location,
                                containerId = containerId,
                                eTag = etag
                            )
                        )
                    }
                )
            )
        }

    fun deleteContainer(containerName: String, eTag: String): Either<RequestError, Boolean> =
        doRequest {
            doDelete(
                request = webTarget.path(W3C).path(containerName)
                    .request()
                    .header("if-match", eTag),
                responseHandlers = mapOf(
                    Response.Status.NO_CONTENT to { Either.Right(true) }
                )
            )
        }

    fun createAnnotation(containerName: String, annotation: Map<String, Any>): Either<RequestError, AnnoRepoResponse> =
        doRequest {
            val request = webTarget.path(W3C).path(containerName).request()
            val response = request.withHeaders()
                .post(Entity.json(annotation))
//        log.info("response={}", response)
            val created = (response.status == HttpStatus.CREATED_201)
            val location = location(response) ?: ""
            val annotationName = extractAnnotationName(location)
            val etag = eTag(response) ?: ""
            Either.Right(AnnoRepoResponse(created, location, containerId = annotationName, eTag = etag))
        }

    fun updateAnnotation(
        containerName: String,
        annotationName: String,
        eTag: String,
        annotation: Map<String, Any>
    ): Either<RequestError, AnnoRepoResponse> =
        doRequest {
            val request = webTarget.path(W3C).path(containerName).path(annotationName).request()
            val response = request
                .header("if-match", eTag)
                .withHeaders()
                .put(Entity.json(annotation))
//        log.info("response={}", response)
            val created = (response.status == HttpStatus.OK_200)
            val location = location(response) ?: ""
            val newEtag = eTag(response) ?: ""
            Either.Right(AnnoRepoResponse(created, location, containerId = annotationName, eTag = newEtag))
        }

    fun deleteAnnotation(containerName: String, annotationName: String, eTag: String): Either<RequestError, Boolean> =
        doRequest {
            val response = webTarget.path(W3C).path(containerName).path(annotationName)
                .request()
                .header("if-match", eTag)
                .withHeaders()
                .delete()
//        log.info("{}", response)
            Either.Right(response.status == NO_CONTENT_204)
        }

    fun getFieldCount(containerName: String): Either<RequestError, Map<String, Int>> =
        doRequest {
            val json = webTarget.path(SERVICES).path(containerName).path(FIELDS)
                .request()
                .withHeaders()
                .get(String::class.java)
            Either.Right(oMapper.readValue(json))
        }

    fun batchUpload(
        containerName: String,
        annotations: List<Map<String, Any>>
    ): Either<RequestError, BatchUploadResponse> =
        doRequest {
            val request = webTarget.path(BATCH).path(containerName).path("annotations").request()
            val response = request.withHeaders()
                .post(Entity.json(annotations))
//        log.info("response={}", response)
            val entityJson: String =
                response.readEntity(String::class.java)
            val annotationData: List<AnnotationIdentifier> = oMapper.readValue(entityJson)
            Either.Right(BatchUploadResponse(annotationData))
        }

    fun createQuery(containerName: String, query: Map<String, Any>): Either<RequestError, String> =
        doRequest {
            val response = webTarget.path(SERVICES).path(containerName).path("search")
                .request()
                .withHeaders()
                .post(Entity.json(query))
            log.info("response={}", response)
            val location = response.location
            Either.Right(location.rawPath.split("/").last())
        }

    fun getQueryResult(containerName: String, queryId: String, page: Int): Either<RequestError, String> =
        doRequest {
            val response =
                webTarget.path(SERVICES).path(containerName).path("search").path(queryId)
                    .queryParam("page", page)
                    .request()
                    .withHeaders()
                    .get()
            Either.Right(response.readEntity(String::class.java))
        }

    fun addIndex(containerName: String, fieldName: String, indexType: IndexType): Either<RequestError, Response> =
        doRequest {
            val entity = Entity.json(emptyMap<String, Any>())
            val response =
                webTarget.path(SERVICES).path(containerName).path("indexes").path(fieldName).path(indexType.name)
                    .request()
                    .withHeaders()
                    .put(entity)
            when (response.status) {
                Response.Status.NO_CONTENT.statusCode -> Either.Right(response)
                Response.Status.UNAUTHORIZED.statusCode -> unauthorizedResponse(response)
                else -> unexpectedResponse(response)
            }
        }

    fun listIndexes(containerName: String): Either<RequestError, String> =
        doRequest {
            val response = webTarget.path(SERVICES).path(containerName).path("indexes")
                .request()
                .withHeaders()
                .get()
            when (response.status) {
                Response.Status.OK.statusCode -> Either.Right(response.readEntity(String::class.java))
                Response.Status.UNAUTHORIZED.statusCode -> unauthorizedResponse(response)
                else -> unexpectedResponse(response)
            }
        }

    fun deleteIndex(containerName: String, fieldName: String, indexType: IndexType): Either<RequestError, Boolean> =
        doRequest {
            val response =
                webTarget.path(SERVICES).path(containerName).path("indexes").path(fieldName).path(indexType.name)
                    .request()
                    .withHeaders()
                    .delete()
            when (response.status) {
                Response.Status.NO_CONTENT.statusCode -> Either.Right(true)
                Response.Status.UNAUTHORIZED.statusCode -> unauthorizedResponse(response)
                else -> unexpectedResponse(response)
            }
        }

    fun getUsers(): Either<RequestError, List<UserEntry>> =
        doRequest {
            val response = webTarget.path(ADMIN).path(USERS)
                .request()
                .withHeaders()
                .get()
            when (response.status) {
                Response.Status.OK.statusCode -> {
                    val json = response.readEntity(String::class.java)
                    val userEntryList = oMapper.readValue(json, object : TypeReference<List<UserEntry>>() {})
                    Either.Right(userEntryList)
                }

                Response.Status.UNAUTHORIZED.statusCode -> unauthorizedResponse(response)

                else -> unexpectedResponse(response)
            }
        }

    // private functions
    private fun <T> doGet(
        request: Invocation.Builder,
        responseHandlers: ResponseHandlerMap<T>
    ): Either<RequestError, T> =
        request
            .withHeaders()
            .get()
            .processResponseWith(responseHandlers)

    private fun <T> doPost(
        request: Invocation.Builder,
        entity: Entity<*>,
        responseHandlers: ResponseHandlerMap<T>
    ): Either<RequestError, T> =
        request
            .withHeaders()
            .post(entity)
            .processResponseWith(responseHandlers)

    private fun <T> doPut(
        request: Invocation.Builder,
        entity: Entity<*>,
        responseHandlers: ResponseHandlerMap<T>
    ): Either<RequestError, T> =
        request
            .withHeaders()
            .put(entity)
            .processResponseWith(responseHandlers)

    private fun <T> doDelete(
        request: Invocation.Builder,
        responseHandlers: ResponseHandlerMap<T>
    ): Either<RequestError, T> =
        request
            .withHeaders()
            .delete()
            .processResponseWith(responseHandlers)

    private fun <T> Response.processResponseWith(
        responseHandlers: Map<Response.Status, (Response) -> Either<RequestError, T>>
    ): Either<RequestError, T> {
        val handlerIdx = responseHandlers.entries.associate { it.key.statusCode to it.value }
        return when (status) {
            in handlerIdx.keys -> handlerIdx[status]!!.invoke(this)

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