package nl.knaw.huc.annorepo.client

import arrow.core.Either
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import nl.knaw.huc.annorepo.api.AboutInfo
import nl.knaw.huc.annorepo.api.AnnotationIdentifier
import nl.knaw.huc.annorepo.api.IndexType
import nl.knaw.huc.annorepo.api.ResourcePaths.ABOUT
import nl.knaw.huc.annorepo.api.ResourcePaths.BATCH
import nl.knaw.huc.annorepo.api.ResourcePaths.FIELDS
import nl.knaw.huc.annorepo.api.ResourcePaths.SERVICES
import nl.knaw.huc.annorepo.api.ResourcePaths.W3C
import nl.knaw.huc.annorepo.client.ARResponse.AnnoRepoResponse
import nl.knaw.huc.annorepo.client.ARResponse.BatchUploadResponse
import nl.knaw.huc.annorepo.client.RequestError.ConnectionError
import nl.knaw.huc.annorepo.util.extractVersion
import org.eclipse.jetty.http.HttpStatus
import org.eclipse.jetty.http.HttpStatus.NO_CONTENT_204
import org.slf4j.LoggerFactory
import java.net.URI
import javax.ws.rs.client.ClientBuilder
import javax.ws.rs.client.Entity
import javax.ws.rs.client.Invocation
import javax.ws.rs.client.WebTarget
import javax.ws.rs.core.Response

class AnnoRepoClient(serverURI: URI, val apiKey: String? = null, private val userAgent: String? = null) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val webTarget: WebTarget = ClientBuilder.newClient()
        .target(serverURI)
    private val oMapper: ObjectMapper = ObjectMapper().registerKotlinModule()

    var serverVersion: String? = null
    var serverNeedsAuthentication: Boolean? = null

    init {
        log.info("checking annorepo server at $serverURI ...")
        getAbout().bimap(
            { e -> log.error("error: {}", e) },
            { aboutInfo ->
                serverVersion = aboutInfo.version
                serverNeedsAuthentication = aboutInfo.withAuthentication
                log.info("$serverURI runs version $serverVersion ; needs authentication: $serverNeedsAuthentication")
            }
        )
    }

    fun getAbout(): Either<RequestError, AboutInfo> =
        try {
            val json = webTarget.path(ABOUT)
                .request()
                .withUserAgent()
                .get(String::class.java)
            Either.Right(oMapper.readValue(json))
        } catch (e: Exception) {
            Either.Left(ConnectionError(e.message ?: "Connection Error"))
        }

    fun createContainer(
        preferredName: String? = null,
        label: String = "A container for web annotations"
    ): AnnoRepoResponse {
        var request = webTarget.path(W3C).request()
        if (preferredName != null) {
            request = request.header("slug", preferredName)
        }
        val specs = containerSpecs(label)
        val response = request.withUserAgent()
            .post(Entity.json(specs))
//        log.info("response={}", response)
        val created = (response.status == HttpStatus.CREATED_201)
        val location = location(response) ?: ""
        val containerId = extractContainerName(location)
        val etag = eTag(response) ?: ""
        return AnnoRepoResponse(created, location, containerId = containerId, eTag = etag)
    }

    fun deleteContainer(containerName: String, eTag: String): Boolean {
        val response = webTarget.path(W3C).path(containerName)
            .request()
            .header("if-match", eTag)
            .withUserAgent()
            .delete()
//        log.info("{}", response)
        return response.status == NO_CONTENT_204
    }

    fun createAnnotation(containerName: String, annotation: Map<String, Any>): AnnoRepoResponse {
        val request = webTarget.path(W3C).path(containerName).request()
        val response = request.withUserAgent()
            .post(Entity.json(annotation))
//        log.info("response={}", response)
        val created = (response.status == HttpStatus.CREATED_201)
        val location = location(response) ?: ""
        val annotationName = extractAnnotationName(location)
        val etag = eTag(response) ?: ""
        return AnnoRepoResponse(created, location, containerId = annotationName, eTag = etag)
    }

    fun updateAnnotation(
        containerName: String,
        annotationName: String,
        eTag: String,
        annotation: Map<String, Any>
    ): AnnoRepoResponse {
        val request = webTarget.path(W3C).path(containerName).path(annotationName).request()
        val response = request
            .header("if-match", eTag)
            .withUserAgent()
            .put(Entity.json(annotation))
//        log.info("response={}", response)
        val created = (response.status == HttpStatus.OK_200)
        val location = location(response) ?: ""
        val newEtag = eTag(response) ?: ""
        return AnnoRepoResponse(created, location, containerId = annotationName, eTag = newEtag)
    }

    fun deleteAnnotation(containerName: String, annotationName: String, eTag: String): Boolean {
        val response = webTarget.path(W3C).path(containerName).path(annotationName)
            .request()
            .header("if-match", eTag)
            .withUserAgent()
            .delete()
//        log.info("{}", response)
        return response.status == NO_CONTENT_204
    }

    fun getFieldCount(containerName: String): Map<String, Int> {
        val json = webTarget.path(SERVICES).path(containerName).path(FIELDS)
            .request()
            .withUserAgent()
            .get(String::class.java)
        return oMapper.readValue(json)
    }

    fun batchUpload(containerName: String, annotations: List<Map<String, Any>>): BatchUploadResponse {
        val request = webTarget.path(BATCH).path(containerName).path("annotations").request()
        val response = request.withUserAgent()
            .post(Entity.json(annotations))
//        log.info("response={}", response)
        val entityJson: String =
            response.readEntity(String::class.java)
        val annotationData: List<AnnotationIdentifier> = oMapper.readValue(entityJson)
        return BatchUploadResponse(annotationData)
    }

    fun createQuery(containerName: String, query: Map<String, Any>): String {
        val request = webTarget.path(SERVICES).path(containerName).path("search").request()
        val response = request.withUserAgent()
            .post(Entity.json(query))
        log.info("response={}", response)
        val location = response.location
        return location.rawPath.split("/").last()
    }

    fun getQueryResult(containerName: String, queryId: String, page: Int): Any {
        val request =
            webTarget.path(SERVICES).path(containerName).path("search").path(queryId).queryParam("page", page).request()
        val response = request.withUserAgent()
            .get()
        return response.readEntity(String::class.java)
    }

    fun addIndex(containerName: String, fieldName: String, indexType: IndexType): Any {
        val request =
            webTarget.path(SERVICES).path(containerName).path("indexes").path(fieldName).path(indexType.name).request()
        return request.withUserAgent()
            .put(null)
    }

    fun listIndexes(containerName: String): String {
        val request =
            webTarget.path(SERVICES).path(containerName).path("indexes").request()
        return request.withUserAgent()
            .get(String::class.java)
    }

    fun deleteIndex(containerName: String, fieldName: String, indexType: IndexType): Boolean {
        val request =
            webTarget.path(SERVICES).path(containerName).path("indexes").path(fieldName).path(indexType.name).request()
        return request.withUserAgent()
            .delete().status == Response.Status.NO_CONTENT.statusCode

    }

    // private functions

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

    private fun Response.firstHeader(key: String): String? =
        if (headers.containsKey(key)) {
            val locations: MutableList<Any> = headers[key]!!
            locations[0].toString()
        } else {
            null
        }

    private fun Invocation.Builder.withUserAgent(): Invocation.Builder {
        val libUA = "${AnnoRepoClient::class.java.name}/${getVersion() ?: ""}"
        val ua = if (userAgent == null) {
            libUA
        } else {
            "$userAgent ( using $libUA )"
        }
        return header("User-Agent", ua)
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