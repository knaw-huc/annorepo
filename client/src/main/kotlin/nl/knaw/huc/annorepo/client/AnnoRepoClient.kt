package nl.knaw.huc.annorepo.client

import java.net.URI
import java.util.PropertyResourceBundle
import javax.ws.rs.client.ClientBuilder
import javax.ws.rs.client.Entity
import javax.ws.rs.client.Invocation
import javax.ws.rs.client.WebTarget
import javax.ws.rs.core.Response
import kotlin.streams.asStream
import arrow.core.Either
import arrow.core.flatMap
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.glassfish.jersey.client.filter.EncodingFilter
import org.glassfish.jersey.message.GZipEncoder
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import nl.knaw.huc.annorepo.api.AnnotationIdentifier
import nl.knaw.huc.annorepo.api.AnnotationPage
import nl.knaw.huc.annorepo.api.ContainerUserEntry
import nl.knaw.huc.annorepo.api.IndexConfig
import nl.knaw.huc.annorepo.api.IndexType
import nl.knaw.huc.annorepo.api.ResourcePaths.ABOUT
import nl.knaw.huc.annorepo.api.ResourcePaths.ADMIN
import nl.knaw.huc.annorepo.api.ResourcePaths.BATCH
import nl.knaw.huc.annorepo.api.ResourcePaths.CONTAINER_SERVICES
import nl.knaw.huc.annorepo.api.ResourcePaths.FIELDS
import nl.knaw.huc.annorepo.api.ResourcePaths.INDEXES
import nl.knaw.huc.annorepo.api.ResourcePaths.INFO
import nl.knaw.huc.annorepo.api.ResourcePaths.METADATA
import nl.knaw.huc.annorepo.api.ResourcePaths.MY
import nl.knaw.huc.annorepo.api.ResourcePaths.SEARCH
import nl.knaw.huc.annorepo.api.ResourcePaths.USERS
import nl.knaw.huc.annorepo.api.ResourcePaths.W3C
import nl.knaw.huc.annorepo.api.SearchInfo
import nl.knaw.huc.annorepo.api.UserAddResults
import nl.knaw.huc.annorepo.api.UserEntry
import nl.knaw.huc.annorepo.client.ARResult.AddIndexResult
import nl.knaw.huc.annorepo.client.ARResult.AddUsersResult
import nl.knaw.huc.annorepo.client.ARResult.AnnotationFieldInfoResult
import nl.knaw.huc.annorepo.client.ARResult.BatchUploadResult
import nl.knaw.huc.annorepo.client.ARResult.ContainerUsersResult
import nl.knaw.huc.annorepo.client.ARResult.CreateAnnotationResult
import nl.knaw.huc.annorepo.client.ARResult.CreateContainerResult
import nl.knaw.huc.annorepo.client.ARResult.CreateSearchResult
import nl.knaw.huc.annorepo.client.ARResult.DeleteAnnotationResult
import nl.knaw.huc.annorepo.client.ARResult.DeleteContainerResult
import nl.knaw.huc.annorepo.client.ARResult.DeleteContainerUserResult
import nl.knaw.huc.annorepo.client.ARResult.DeleteIndexResult
import nl.knaw.huc.annorepo.client.ARResult.DeleteUserResult
import nl.knaw.huc.annorepo.client.ARResult.GetAboutResult
import nl.knaw.huc.annorepo.client.ARResult.GetAnnotationResult
import nl.knaw.huc.annorepo.client.ARResult.GetContainerMetadataResult
import nl.knaw.huc.annorepo.client.ARResult.GetContainerResult
import nl.knaw.huc.annorepo.client.ARResult.GetIndexResult
import nl.knaw.huc.annorepo.client.ARResult.GetSearchInfoResult
import nl.knaw.huc.annorepo.client.ARResult.GetSearchResultPageResult
import nl.knaw.huc.annorepo.client.ARResult.ListIndexesResult
import nl.knaw.huc.annorepo.client.ARResult.MyContainersResult
import nl.knaw.huc.annorepo.client.ARResult.UsersResult
import nl.knaw.huc.annorepo.client.RequestError.ConnectionError

private const val IF_MATCH = "if-match"

/**
 * Client to access annorepo servers.
 *
 * @constructor
 * @param serverURI the server *URI*
 * @param apiKey the api-key for authentication (optional)
 * @param userAgent the string to identify this client in the User-Agent header (optional)
 *
 */
class AnnoRepoClient @JvmOverloads constructor(
    serverURI: URI, val apiKey: String? = null, private val userAgent: String? = null,
) {

    private val webTarget: WebTarget = ClientBuilder.newClient().apply {
        register(GZipEncoder::class.java)
        register(EncodingFilter::class.java)
    }.target(serverURI)

    lateinit var serverVersion: String
    var serverNeedsAuthentication: Boolean? = null

    init {
        log.info("checking annorepo server at $serverURI ...")
        getAbout().fold(
            { e ->
                log.error("error: {}", e)
                throw RuntimeException("Unable to connect to annorepo server at $serverURI")
            },
            { getAboutResult ->
                val aboutInfo = getAboutResult.aboutInfo
                serverVersion = aboutInfo.version
                serverNeedsAuthentication = aboutInfo.withAuthentication
                log.info("$serverURI runs version $serverVersion ; needs authentication: $serverNeedsAuthentication")
            })
    }

    /**
     * Get about
     *
     * @return
     */
    fun getAbout(): Either<RequestError, GetAboutResult> = doGet(
        request = webTarget.path(ABOUT).request(),
        responseHandlers = mapOf(Response.Status.OK to { response: Response ->
            val json = response.readEntityAsJsonString()
            Either.Right(GetAboutResult(response, oMapper.readValue(json)))
        })
    )

    /**
     * Create an annotation container
     *
     * @param preferredName the preferred name of the container. May be overridden by the server
     * @param label a short, human-readable description of this container
     * @return
     */
    @JvmOverloads
    fun createContainer(
        preferredName: String? = null,
        label: String = "A container for web annotations",
    ): Either<RequestError, CreateContainerResult> {
        var request = webTarget.path(W3C).request()
        if (preferredName != null) {
            request = request.header("slug", preferredName)
        }
        return doPost(
            request = request,
            entity = Entity.json(containerSpecs(label)),
            responseHandlers = mapOf(Response.Status.CREATED to { response ->
                val location = response.location()!!
                val containerName = extractContainerName(location.toString())
                val eTag = response.eTag() ?: ""
                Either.Right(
                    CreateContainerResult(
                        response = response, location = location, containerName = containerName, eTag = eTag
                    )
                )
            })
        )
    }

    /**
     * Get an annotation container
     *
     * @param containerName
     * @return
     */
    fun getContainer(
        containerName: String,
    ): Either<RequestError, GetContainerResult> = doGet(
        request = webTarget.path(W3C).path(containerName).request(),
        responseHandlers = mapOf(Response.Status.OK to { response ->
            val json = response.readEntityAsJsonString()
            Either.Right(
                GetContainerResult(
                    response = response,
                    eTag = response.eTag().toString(),
                    entity = json
                )
            )
        })
    )

    /**
     * Get annotation container metadata
     *
     * @param containerName
     * @return
     */
    fun getContainerMetadata(
        containerName: String,
    ): Either<RequestError, GetContainerMetadataResult> = doGet(
        request = webTarget.path(CONTAINER_SERVICES).path(containerName).path(METADATA).request(),
        responseHandlers = mapOf(Response.Status.OK to { response ->
            val json = response.readEntityAsJsonString()
            val metadata: Map<String, Any> = oMapper.readValue(json)
            Either.Right(
                GetContainerMetadataResult(
                    response = response, metadata = metadata
                )
            )
        })
    )

    /**
     * Delete an annotation container
     *
     * @param containerName
     * @param eTag
     * @return
     */
    fun deleteContainer(containerName: String, eTag: String): Either<RequestError, DeleteContainerResult> = doDelete(
        request = webTarget.path(W3C).path(containerName).request().header(IF_MATCH, eTag),
        responseHandlers = mapOf(Response.Status.NO_CONTENT to { response ->
            Either.Right(
                DeleteContainerResult(
                    response = response
                )
            )
        })
    )

    /**
     * Create annotation
     *
     * @param containerName
     * @param annotation
     * @return
     */
    fun createAnnotation(
        containerName: String, annotation: Any,
    ): Either<RequestError, CreateAnnotationResult> = doPost(
        request = webTarget.path(W3C).path(containerName).request(),
        entity = Entity.json(annotation),
        responseHandlers = mapOf(Response.Status.CREATED to { response ->
            val location = response.location()!!
            val annotationName = extractAnnotationName(location.toString())
            val eTag = response.eTag() ?: ""
            Either.Right(
                CreateAnnotationResult(
                    response = response,
                    location = location,
                    containerName = containerName,
                    annotationName = annotationName,
                    eTag = eTag
                )
            )
        })
    )

    /**
     * Read annotation
     *
     * @param containerName
     * @param annotationName
     * @return
     */
    fun getAnnotation(
        containerName: String, annotationName: String,
    ): Either<RequestError, GetAnnotationResult> = doGet(
        request = webTarget.path(W3C).path(containerName).path(annotationName).request(),
        responseHandlers = mapOf(Response.Status.OK to { response ->
            val eTag = response.eTag() ?: ""
            val json = response.readEntityAsJsonString()
            val annotation: Map<String, Any> = oMapper.readValue(json)
            Either.Right(
                GetAnnotationResult(
                    response = response,
                    eTag = eTag,
                    annotation = annotation,
                )
            )
        })
    )

    /**
     * Update annotation
     *
     * @param containerName
     * @param annotationName
     * @param eTag
     * @param annotation
     * @return
     */
    fun updateAnnotation(
        containerName: String, annotationName: String, eTag: String, annotation: Any,
    ): Either<RequestError, CreateAnnotationResult> {
        val path = webTarget.path(W3C).path(containerName).path(annotationName)
        val location = path.uri
        return doPut(
            request = path.request().header(IF_MATCH, eTag),
            entity = Entity.json(annotation),
            responseHandlers = mapOf(Response.Status.OK to { response ->
                val newETag = response.eTag() ?: ""
                Either.Right(
                    CreateAnnotationResult(
                        response = response,
                        location = location,
                        containerName = containerName,
                        annotationName = annotationName,
                        eTag = newETag
                    )
                )
            })
        )
    }

    /**
     * Delete annotation
     *
     * @param containerName
     * @param annotationName
     * @param eTag
     * @return
     */
    fun deleteAnnotation(
        containerName: String, annotationName: String, eTag: String,
    ): Either<RequestError, DeleteAnnotationResult> = doDelete(
        request = webTarget.path(W3C).path(containerName).path(annotationName).request().header(IF_MATCH, eTag),
        responseHandlers = mapOf(Response.Status.NO_CONTENT to { response ->
            Either.Right(
                DeleteAnnotationResult(response)
            )
        })
    )

    /**
     * Get field info
     *
     * @param containerName
     * @return
     */
    fun getFieldInfo(containerName: String): Either<RequestError, AnnotationFieldInfoResult> = doGet(
        request = webTarget.path(CONTAINER_SERVICES).path(containerName).path(FIELDS).request(),
        responseHandlers = mapOf(Response.Status.OK to { response ->
            val json = response.readEntityAsJsonString()
            Either.Right(
                AnnotationFieldInfoResult(
                    response = response, fieldInfo = oMapper.readValue(json)
                )
            )
        })
    )

    /**
     * Batch upload
     *
     * @param containerName
     * @param annotations
     * @return
     */
    fun batchUpload(
        containerName: String, annotations: List<Any>,
    ): Either<RequestError, BatchUploadResult> = doPost(
        request = webTarget.path(BATCH).path(containerName).path("annotations").request(),
        entity = Entity.json(annotations),
        responseHandlers = mapOf(Response.Status.OK to { response ->
            val entityJson: String = response.readEntityAsJsonString()
            val annotationData: List<AnnotationIdentifier> = oMapper.readValue(entityJson)
            Either.Right(
                BatchUploadResult(response, annotationData)
            )
        })
    )

    /**
     * Create search
     *
     * @param containerName
     * @param query
     * @return
     */
    fun createSearch(containerName: String, query: Map<String, Any>): Either<RequestError, CreateSearchResult> = doPost(
        request = webTarget.path(CONTAINER_SERVICES).path(containerName).path(SEARCH).request(),
        entity = Entity.json(query),
        responseHandlers = mapOf(Response.Status.CREATED to { response ->
            val location = response.location
            val queryId = location.rawPath.split("/").last()
            Either.Right(
                CreateSearchResult(response = response, location = location, queryId = queryId)
            )
        })
    )

    /**
     * Get search result page
     *
     * @param containerName
     * @param queryId
     * @param page
     * @return
     */
    fun getSearchResultPage(
        containerName: String, queryId: String, page: Int,
    ): Either<RequestError, GetSearchResultPageResult> =
        doGet(
            request = webTarget.path(CONTAINER_SERVICES).path(containerName).path(SEARCH).path(queryId)
                .queryParam("page", page)
                .request(), responseHandlers = mapOf(
                Response.Status.OK to { response ->
                    val json = response.readEntityAsJsonString()
                    val annotationPage: AnnotationPage = oMapper.readValue(json)
                    Either.Right(
                        GetSearchResultPageResult(
                            response = response, annotationPage = annotationPage
                        )
                    )
                })
        )

    /**
     * Get search info
     *
     * @param containerName
     * @param queryId
     * @return
     */
    fun getSearchInfo(containerName: String, queryId: String): Either<RequestError, GetSearchInfoResult> =
        doGet(
            request = webTarget.path(CONTAINER_SERVICES).path(containerName).path(SEARCH).path(queryId).path(INFO)
                .request(),
            responseHandlers = mapOf(Response.Status.OK to { response ->
                val json = response.readEntityAsJsonString()
                val searchInfo: SearchInfo = oMapper.readValue(json)
                Either.Right(
                    GetSearchInfoResult(response, searchInfo)
                )
            })
        )

    /**
     * Filter container annotations
     *
     * @param containerName
     * @param query
     * @return
     */
    fun filterContainerAnnotations(
        containerName: String, query: Map<String, Any>,
    ): Either<RequestError, FilterContainerAnnotationsResult> =
        createSearch(containerName, query)
            .flatMap { createSearchResult ->
                val queryId = createSearchResult.queryId
                val annotationSequence = annotationSequence(containerName, queryId)
                Either.Right(
                    FilterContainerAnnotationsResult(
                        queryId = queryId,
                        annotations = annotationSequence.asStream()
                    )
                )
            }

    /**
     * Filter container annotations2
     *
     * @param containerName
     * @param query
     * @return
     */
    fun filterContainerAnnotations2(
        containerName: String, query: Map<String, Any>,
    ): Sequence<Either<RequestError, String>> =
        createSearch(containerName, query).fold(
            { e -> sequenceOf(Either.Left(e)) },
            { createSearchResult ->
                val queryId = createSearchResult.queryId
                annotationSequence(containerName, queryId)
            }
        )

    private fun annotationSequence(
        containerName: String,
        queryId: String,
    ): Sequence<Either<RequestError, String>> =
        sequence {
            var page = 0
            var goOn = true
            while (goOn) {
                goOn = getSearchResultPage(containerName, queryId, page)
                    .fold(
                        { error ->
                            yield(Either.Left(error))
                            false
                        },
                        { result ->
                            yieldAll(result.annotationPage.items.map {
                                val jsonString = oMapper.writeValueAsString(it)
                                Either.Right(jsonString)
                            })
                            result.annotationPage.next != null
                        }
                    )
                page += 1
            }
        }

    /**
     * Add index
     *
     * @param containerName
     * @param fieldName
     * @param indexType
     * @return
     */
    fun addIndex(containerName: String, fieldName: String, indexType: IndexType): Either<RequestError, AddIndexResult> =
        doPut(
            request = webTarget.path(CONTAINER_SERVICES).path(containerName).path(INDEXES).path(fieldName)
                .path(indexType.name)
                .request(),
            entity = Entity.json(emptyMap<String, Any>()),
            responseHandlers = mapOf(Response.Status.CREATED to { response ->
                Either.Right(AddIndexResult(response = response))
            })
        )

    /**
     * Get index
     *
     * @param containerName
     * @param fieldName
     * @param indexType
     * @return
     */
    fun getIndex(containerName: String, fieldName: String, indexType: IndexType): Either<RequestError, GetIndexResult> =
        doGet(
            request = webTarget.path(CONTAINER_SERVICES).path(containerName).path(INDEXES).path(fieldName)
                .path(indexType.name)
                .request(), responseHandlers = mapOf(Response.Status.OK to { response ->
                val json = response.readEntityAsJsonString()
                val indexConfig: IndexConfig = oMapper.readValue(json)
                Either.Right(
                    GetIndexResult(response, indexConfig)
                )
            })
        )

    /**
     * List indexes
     *
     * @param containerName
     * @return
     */
    fun listIndexes(containerName: String): Either<RequestError, ListIndexesResult> = doGet(
        request = webTarget.path(CONTAINER_SERVICES).path(containerName).path(INDEXES).request(),
        responseHandlers = mapOf(Response.Status.OK to { response ->
            val jsonString = response.readEntityAsJsonString()
            val indexes: List<IndexConfig> = oMapper.readValue(jsonString)
            Either.Right(
                ListIndexesResult(
                    response = response, indexes = indexes
                )
            )
        })
    )

    /**
     * Delete index
     *
     * @param containerName the name of the container
     * @param fieldName the name of the indexed field
     * @param indexType the type of index
     * @return
     */
    fun deleteIndex(
        containerName: String, fieldName: String, indexType: IndexType,
    ): Either<RequestError, DeleteIndexResult> = doDelete(
        request = webTarget.path(CONTAINER_SERVICES).path(containerName).path(INDEXES).path(fieldName)
            .path(indexType.name)
            .request(), responseHandlers = mapOf(Response.Status.NO_CONTENT to { response ->
            Either.Right(DeleteIndexResult(response))
        })
    )

    /**
     * Get users
     *
     * @return
     */
    fun getUsers(): Either<RequestError, UsersResult> = doGet(
        request = webTarget.path(ADMIN).path(USERS).request(),
        responseHandlers = mapOf(Response.Status.OK to { response ->
            val json = response.readEntityAsJsonString()
            val userEntryList = oMapper.readValue(json, object : TypeReference<List<UserEntry>>() {})
            Either.Right(
                UsersResult(
                    response = response, userEntries = userEntryList
                )
            )
        })
    )

    /**
     * Add users
     *
     * @param users
     * @return
     */
    fun addUsers(users: List<UserEntry>): Either<RequestError, AddUsersResult> = doPost(
        request = webTarget.path(ADMIN).path(USERS).request(),
        entity = Entity.json(users),
        responseHandlers = mapOf(
            Response.Status.OK to { response ->
                val json = response.readEntityAsJsonString()
                val userAddResults: UserAddResults = oMapper.readValue(json)
                Either.Right(
                    AddUsersResult(
                        response = response,
                        accepted = userAddResults.added,
                        rejected = userAddResults.rejected
                    )
                )
            })
    )

    /**
     * Delete user
     *
     * @param userName
     * @return
     */
    fun deleteUser(userName: String): Either<RequestError, DeleteUserResult> = doDelete(
        request = webTarget.path(ADMIN).path(USERS).path(userName).request(),
        responseHandlers = mapOf(Response.Status.NO_CONTENT to { response ->
            Either.Right(
                DeleteUserResult(response)
            )
        })
    )

    /**
     * Get container users
     *
     * @param containerName
     * @return
     */
    fun getContainerUsers(containerName: String): Either<RequestError, ContainerUsersResult> = doGet(
        request = webTarget.path(CONTAINER_SERVICES).path(containerName).path(USERS).request(),
        responseHandlers = mapOf(Response.Status.OK to { response ->
            val json = response.readEntityAsJsonString()
            val containerUserEntryList = oMapper.readValue(json, object : TypeReference<List<ContainerUserEntry>>() {})
            Either.Right(
                ContainerUsersResult(
                    response = response, containerUserEntries = containerUserEntryList
                )
            )
        })
    )

    /**
     * Add container users
     *
     * @param containerName
     * @param containerUserEntries
     * @return
     */
    fun addContainerUsers(
        containerName: String,
        containerUserEntries: List<ContainerUserEntry>,
    ): Either<RequestError, ContainerUsersResult> = doPost(
        request = webTarget.path(CONTAINER_SERVICES).path(containerName).path(USERS).request(),
        entity = Entity.json(containerUserEntries),
        responseHandlers = mapOf(
            Response.Status.OK to { response ->
                val json = response.readEntityAsJsonString()
                val containerUserEntryList =
                    oMapper.readValue(json, object : TypeReference<List<ContainerUserEntry>>() {})
                Either.Right(
                    ContainerUsersResult(
                        response = response,
                        containerUserEntries = containerUserEntryList
                    )
                )
            })
    )

    /**
     * Delete container user
     *
     * @param containerName
     * @param userName
     * @return
     */
    fun deleteContainerUser(
        containerName: String,
        userName: String,
    ): Either<RequestError, DeleteContainerUserResult> = doDelete(
        request = webTarget.path(CONTAINER_SERVICES).path(containerName).path(USERS).path(userName).request(),
        responseHandlers = mapOf(
            Response.Status.OK to { response ->
                Either.Right(
                    DeleteContainerUserResult(
                        response = response
                    )
                )
            })
    )

    fun getMyContainers(): Either<RequestError, MyContainersResult> = doGet(
        request = webTarget.path(MY).path("containers").request(),
        responseHandlers = mapOf(
            Response.Status.OK to { response ->
                val json = response.readEntityAsJsonString()
                val accessibleContainers =
                    oMapper.readValue(json, object : TypeReference<Map<String, List<String>>>() {})
                Either.Right(
                    MyContainersResult(
                        response = response,
                        containers = accessibleContainers
                    )
                )
            })
    )

    // private functions
    private fun <T> doGet(
        request: Invocation.Builder, responseHandlers: ResponseHandlerMap<T>,
    ): Either<RequestError, T> = doRequest {
        request.withHeaders().get().processResponseWith(responseHandlers)
    }

    private fun <T> doPost(
        request: Invocation.Builder, entity: Entity<*>, responseHandlers: ResponseHandlerMap<T>,
    ): Either<RequestError, T> = doRequest {
        request.withHeaders().post(entity).processResponseWith(responseHandlers)
    }

    private fun <T> doPut(
        request: Invocation.Builder, entity: Entity<*>, responseHandlers: ResponseHandlerMap<T>,
    ): Either<RequestError, T> = doRequest {
        request.withHeaders().put(entity).processResponseWith(responseHandlers)
    }

    private fun <T> doDelete(
        request: Invocation.Builder, responseHandlers: ResponseHandlerMap<T>,
    ): Either<RequestError, T> = doRequest {
        request.withHeaders().delete().processResponseWith(responseHandlers)
    }

    private fun <T> Response.processResponseWith(
        responseHandlers: Map<Response.Status, (Response) -> Either<RequestError, T>>,
    ): Either<RequestError, T> {
        val handlerIdx = responseHandlers.entries.associate { it.key.statusCode to it.value }
        return when (status) {
            in handlerIdx.keys -> handlerIdx[status]!!.invoke(this)
            Response.Status.UNAUTHORIZED.statusCode -> unauthorizedResponse(this)
            else -> unexpectedResponse(this)
        }
    }

    private fun unauthorizedResponse(response: Response): Either.Left<RequestError> = Either.Left(
        RequestError.NotAuthorized(
            message = "Not authorized to make this call; check your apiKey",
            response = response,
            responseString = response.readEntityAsJsonString()
        )
    )

    private fun unexpectedResponse(response: Response): Either.Left<RequestError> = Either.Left(
        RequestError.UnexpectedResponse(
            message = "Unexpected status: ${response.status}",
            response = response,
            responseString = response.readEntityAsJsonString()
        )
    )

    private fun Response.readEntityAsJsonString(): String = readEntity(String::class.java) ?: ""

    private fun extractContainerName(location: String): String {
        val parts = location.split("/")
        return parts[parts.size - 2]
    }

    private fun extractAnnotationName(location: String): String {
        val parts = location.split("/")
        return parts[parts.size - 1]
    }

    private fun Response.location(): URI? {
        val firstHeader = firstHeader("location") ?: return null
        return URI.create(firstHeader)
    }

    private fun Response.eTag(): String? = firstHeader("etag")

    private fun <R> doRequest(requestHandler: () -> Either<RequestError, R>): Either<RequestError, R> =
        try {
            requestHandler()
        } catch (e: Exception) {
            e.printStackTrace()
            Either.Left(ConnectionError(e.message ?: e.javaClass.name))
        }

    private fun Response.firstHeader(key: String): String? = if (headers.containsKey(key)) {
        val locations: MutableList<Any> = headers[key]!!
        locations[0].toString()
    } else {
        null
    }

    private fun Invocation.Builder.withHeaders(): Invocation.Builder {
        val libUA = "${AnnoRepoClient::class.java.name}/${classVersion}"
        val ua = if (userAgent == null) {
            libUA
        } else {
            "$userAgent ( using $libUA )"
        }
        var builder =
            header("User-Agent", ua).header("Accept-Encoding", "gzip").header("Content-Encoding", "gzip")

        if (serverNeedsAuthentication != null && serverNeedsAuthentication!!) {
            builder = builder.header("Authorization", "Bearer $apiKey")
        }
        return builder
    }

    private fun containerSpecs(label: String) = mapOf(
        "@context" to listOf(
            "http://www.w3.org/ns/anno.jsonld", "http://www.w3.org/ns/ldp.jsonld"
        ), "type" to listOf(
            "BasicContainer", "AnnotationCollection"
        ), "label" to label
    )

    companion object {
        private val log: Logger = LoggerFactory.getLogger(AnnoRepoClient::class.java)
        private val oMapper: ObjectMapper = ObjectMapper().registerKotlinModule()
        private const val PROPERTY_FILE = "annorepo-client.properties"

        private val classVersion: String by lazy {
            val resourceAsStream = AnnoRepoClient::class.java.getResourceAsStream(PROPERTY_FILE)
            PropertyResourceBundle(resourceAsStream).getString("version")
        }

        @JvmStatic
        @JvmOverloads
        fun create(serverURL: String, apiKey: String? = null, userAgent: String? = null): AnnoRepoClient? =
            create(URI.create(serverURL), apiKey, userAgent)

        @JvmStatic
        @JvmOverloads
        fun create(serverURI: URI, apiKey: String? = null, userAgent: String? = null): AnnoRepoClient? =
            try {
                val annoRepoClient =
                    AnnoRepoClient(serverURI = serverURI, apiKey = apiKey, userAgent = userAgent)
                if (annoRepoClient.serverNeedsAuthentication!! && apiKey == null) {
                    log.warn(
                        "The server at $serverURI has authentication enabled," +
                                " and you did not provide an apiKey." +
                                " You'll only be able to access the endpoints that don't require authentication."
                    )
                }
                annoRepoClient
            } catch (e: RuntimeException) {
                null
            }

    }

}