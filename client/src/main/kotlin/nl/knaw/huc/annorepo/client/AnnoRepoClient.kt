package nl.knaw.huc.annorepo.client

import java.net.URI
import java.util.PropertyResourceBundle
import jakarta.ws.rs.client.ClientBuilder
import jakarta.ws.rs.client.Entity
import jakarta.ws.rs.client.Invocation
import jakarta.ws.rs.client.WebTarget
import jakarta.ws.rs.core.Response
import kotlin.io.encoding.Base64.Default.encode
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.streams.asStream
import arrow.core.Either
import arrow.core.flatMap
import arrow.core.raise.either
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import kotlinx.coroutines.delay
import org.apache.logging.log4j.kotlin.logger
import org.glassfish.jersey.client.filter.EncodingFilter
import org.glassfish.jersey.message.GZipEncoder
import nl.knaw.huc.annorepo.api.AnnotationIdentifier
import nl.knaw.huc.annorepo.api.AnnotationPage
import nl.knaw.huc.annorepo.api.ChoreStatusSummary
import nl.knaw.huc.annorepo.api.ContainerUserEntry
import nl.knaw.huc.annorepo.api.CustomQuerySpecs
import nl.knaw.huc.annorepo.api.IndexConfig
import nl.knaw.huc.annorepo.api.IndexType
import nl.knaw.huc.annorepo.api.MetadataMap
import nl.knaw.huc.annorepo.api.QueryAsMap
import nl.knaw.huc.annorepo.api.ResourcePaths.ABOUT
import nl.knaw.huc.annorepo.api.ResourcePaths.ADMIN
import nl.knaw.huc.annorepo.api.ResourcePaths.ANNOTATIONS_BATCH
import nl.knaw.huc.annorepo.api.ResourcePaths.CONTAINERS
import nl.knaw.huc.annorepo.api.ResourcePaths.CONTAINER_SERVICES
import nl.knaw.huc.annorepo.api.ResourcePaths.CUSTOM_QUERY
import nl.knaw.huc.annorepo.api.ResourcePaths.DISTINCT_FIELD_VALUES
import nl.knaw.huc.annorepo.api.ResourcePaths.EXPAND
import nl.knaw.huc.annorepo.api.ResourcePaths.FIELDS
import nl.knaw.huc.annorepo.api.ResourcePaths.GLOBAL_SERVICES
import nl.knaw.huc.annorepo.api.ResourcePaths.INDEXES
import nl.knaw.huc.annorepo.api.ResourcePaths.INFO
import nl.knaw.huc.annorepo.api.ResourcePaths.METADATA
import nl.knaw.huc.annorepo.api.ResourcePaths.MY
import nl.knaw.huc.annorepo.api.ResourcePaths.PROFILE
import nl.knaw.huc.annorepo.api.ResourcePaths.READ_ONLY_FOR_ANONYMOUS
import nl.knaw.huc.annorepo.api.ResourcePaths.SEARCH
import nl.knaw.huc.annorepo.api.ResourcePaths.SETTINGS
import nl.knaw.huc.annorepo.api.ResourcePaths.STATUS
import nl.knaw.huc.annorepo.api.ResourcePaths.USERS
import nl.knaw.huc.annorepo.api.ResourcePaths.W3C
import nl.knaw.huc.annorepo.api.SearchInfo
import nl.knaw.huc.annorepo.api.SearchStatusSummary
import nl.knaw.huc.annorepo.api.UserAddResults
import nl.knaw.huc.annorepo.api.UserEntry
import nl.knaw.huc.annorepo.api.WebAnnotationAsMap
import nl.knaw.huc.annorepo.client.ARResult.AddIndexResult
import nl.knaw.huc.annorepo.client.ARResult.AddUsersResult
import nl.knaw.huc.annorepo.client.ARResult.AnnotationFieldInfoResult
import nl.knaw.huc.annorepo.client.ARResult.BatchUploadResult
import nl.knaw.huc.annorepo.client.ARResult.ContainerUsersResult
import nl.knaw.huc.annorepo.client.ARResult.CreateAnnotationResult
import nl.knaw.huc.annorepo.client.ARResult.CreateContainerResult
import nl.knaw.huc.annorepo.client.ARResult.CreateCustomQueryResult
import nl.knaw.huc.annorepo.client.ARResult.CreateSearchResult
import nl.knaw.huc.annorepo.client.ARResult.DeleteResult
import nl.knaw.huc.annorepo.client.ARResult.DistinctAnnotationFieldValuesResult
import nl.knaw.huc.annorepo.client.ARResult.FilterContainerAnnotationsResult
import nl.knaw.huc.annorepo.client.ARResult.GetAboutResult
import nl.knaw.huc.annorepo.client.ARResult.GetAnnotationResult
import nl.knaw.huc.annorepo.client.ARResult.GetContainerMetadataResult
import nl.knaw.huc.annorepo.client.ARResult.GetContainerResult
import nl.knaw.huc.annorepo.client.ARResult.GetIndexCreationStatusResult
import nl.knaw.huc.annorepo.client.ARResult.GetIndexResult
import nl.knaw.huc.annorepo.client.ARResult.GetSearchInfoResult
import nl.knaw.huc.annorepo.client.ARResult.GetSearchResultPageResult
import nl.knaw.huc.annorepo.client.ARResult.ListIndexesResult
import nl.knaw.huc.annorepo.client.ARResult.MyContainersResult
import nl.knaw.huc.annorepo.client.ARResult.MyProfileResult
import nl.knaw.huc.annorepo.client.ARResult.SetAnonymousUserReadAccessResult
import nl.knaw.huc.annorepo.client.ARResult.UsersResult
import nl.knaw.huc.annorepo.client.RequestError.ConnectionError

private const val IF_MATCH = "if-match"

/**
 * Client to access annorepo servers.
 *
 * @param serverURI the server *URI*
 * @param apiKey the api-key for authentication (optional)
 * @param userAgent the string to identify this client in the User-Agent
 *    header (optional)
 * @constructor
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
    private var grpcPort: Int? = null
    private lateinit var grpcHost: String

    init {
        logger.info { "checking annorepo server at $serverURI ..." }
        getAbout().fold(
            { e ->
                logger.error { "error: $e" }
                throw RuntimeException("Unable to connect to annorepo server at $serverURI")
            },
            { getAboutResult ->
                val aboutInfo = getAboutResult.aboutInfo
                serverVersion = aboutInfo["version"]?.toString() ?: "unknown"
                serverNeedsAuthentication = aboutInfo["withAuthentication"].toString().toBoolean()
                grpcPort = aboutInfo["grpcPort"]?.toString()?.toInt()
//                grpcHost = URI(aboutInfo.baseURI).host
                grpcHost = aboutInfo["grpcHostName"]?.toString() ?: URI(aboutInfo["baseURI"].toString()).host
                logger.info { "$serverURI runs version $serverVersion ; needs authentication: $serverNeedsAuthentication; gRPC port: ${grpcPort ?: "unknown"}" }
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
     * @param preferredName the preferred name of the container. May be
     *    overridden by the server
     * @param label a short, human-readable description of this container
     * @return
     */
    @JvmOverloads
    fun createContainer(
        preferredName: String? = null,
        label: String = "A container for web annotations",
        readOnlyForAnonymousUsers: Boolean = false
    ): Either<RequestError, CreateContainerResult> {
        var request = webTarget.path(W3C).request()
        if (preferredName != null) {
            request = request.header("slug", preferredName)
        }
        return doPost(
            request = request,
            entity = Entity.json(containerSpecs(label, readOnlyForAnonymousUsers)),
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
            val metadata: MetadataMap = oMapper.readValue(json)
            Either.Right(
                GetContainerMetadataResult(
                    response = response, metadata = metadata
                )
            )
        })
    )

    /**
     * Set read-only access for anonymous user for the given container
     *
     * @param containerName
     * @return
     */
    fun setAnonymousUserReadAccess(
        containerName: String,
        readOnlyAccess: Boolean
    ): Either<RequestError, SetAnonymousUserReadAccessResult> = doPut(
        request = webTarget
            .path(CONTAINER_SERVICES).path(containerName).path(SETTINGS).path(READ_ONLY_FOR_ANONYMOUS)
            .request(),
        entity = Entity.json(readOnlyAccess),
        responseHandlers = mapOf(Response.Status.OK to { response ->
            Either.Right(
                SetAnonymousUserReadAccessResult(response = response)
            )
        })
    )

    /**
     * Delete an annotation container
     *
     * @param containerName
     * @param eTag
     * @param force
     * @return
     */
    fun deleteContainer(
        containerName: String,
        eTag: String,
        force: Boolean = false
    ): Either<RequestError, DeleteResult> {
        val initialPath = webTarget.path(W3C).path(containerName)
        val path = if (force) initialPath.queryParam("force", true) else initialPath
        return doDelete(
            request = path.request().header(IF_MATCH, eTag),
            responseHandlers = mapOf(Response.Status.NO_CONTENT to { response ->
                Either.Right(
                    DeleteResult(
                        response = response
                    )
                )
            })
        )
    }

    /**
     * Create annotation
     *
     * @param containerName
     * @param annotation
     * @return
     */
    fun createAnnotation(
        containerName: String,
        annotation: WebAnnotationAsMap,
        preferredAnnotationName: String? = null,
    ): Either<RequestError, CreateAnnotationResult> {
        val target = webTarget.path(W3C).path(containerName)
        val request = if (preferredAnnotationName != null) {
            target.request().header("slug", preferredAnnotationName)
        } else {
            target.request()
        }
        return doPost(
            request = request,
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
    }

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
            val annotation: WebAnnotationAsMap = oMapper.readValue(json)
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
        containerName: String, annotationName: String, eTag: String, annotation: WebAnnotationAsMap,
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
    ): Either<RequestError, DeleteResult> = doDelete(
        request = webTarget.path(W3C).path(containerName).path(annotationName).request().header(IF_MATCH, eTag),
        responseHandlers = mapOf(Response.Status.NO_CONTENT to { response ->
            Either.Right(
                DeleteResult(response)
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
     * Get distinct field values
     *
     * @param containerName
     * @param fieldName
     * @return
     */
    fun getDistinctFieldValues(
        containerName: String,
        fieldName: String
    ): Either<RequestError, DistinctAnnotationFieldValuesResult> = doGet(
        request = webTarget.path(CONTAINER_SERVICES).path(containerName).path(DISTINCT_FIELD_VALUES).path(fieldName)
            .request(),
        responseHandlers = mapOf(Response.Status.OK to { response ->
            val json = response.readEntityAsJsonString()
            Either.Right(
                DistinctAnnotationFieldValuesResult(
                    response = response, distinctValues = oMapper.readValue(json)
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
        containerName: String, annotations: List<WebAnnotationAsMap>,
    ): Either<RequestError, BatchUploadResult> = doPost(
        request = webTarget.path(CONTAINER_SERVICES).path(containerName).path(ANNOTATIONS_BATCH).request(),
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
    fun createSearch(containerName: String, query: QueryAsMap): Either<RequestError, CreateSearchResult> = doPost(
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
                .request(),
            responseHandlers = mapOf(
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
        containerName: String, query: QueryAsMap,
    ): Either<RequestError, FilterContainerAnnotationsResult> =
        createSearch(containerName, query)
            .flatMap { createSearchResult ->
                val queryId = createSearchResult.queryId
                val annotationSequence = annotationSequence(containerName, queryId)
                Either.Right(
                    FilterContainerAnnotationsResult(
                        response = createSearchResult.response,
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
        containerName: String, query: QueryAsMap,
    ): Sequence<Either<RequestError, String>> =
        createSearch(containerName, query).fold(
            { e -> sequenceOf(Either.Left(e)) },
            { createSearchResult ->
                val queryId = createSearchResult.queryId
                annotationSequence(containerName, queryId)
            }
        )

    /**
     * Create global search
     *
     * @param query
     * @return
     */
    fun createGlobalSearch(query: QueryAsMap): Either<RequestError, CreateSearchResult> = doPost(
        request = webTarget.path(GLOBAL_SERVICES).path(SEARCH).request(),
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
     * Get global search result page
     *
     * @param queryId
     * @param page
     * @return
     */
    fun getGlobalSearchResultPage(
        queryId: String,
        page: Int,
        retryUntilDone: Boolean = true
    ): Either<RequestError, GetSearchResultPageResult> {
        var result = tryGetGlobalSearchResultPage(queryId, page)
        return if (retryUntilDone) {
            while (result.isLeft()) {
                Thread.sleep(1000)
                result = tryGetGlobalSearchResultPage(queryId, page)
                logger.info { result }
            }
            result
        } else {
            result
        }
    }

    private fun tryGetGlobalSearchResultPage(
        queryId: String,
        page: Int
    ): Either<RequestError, GetSearchResultPageResult> =
        doGet(
            request = webTarget.path(GLOBAL_SERVICES).path(SEARCH).path(queryId)
                .queryParam("page", page)
                .request(),
            responseHandlers = mapOf(
                Response.Status.OK to { response ->
                    val json = response.readEntityAsJsonString()
                    val annotationPage: AnnotationPage = oMapper.readValue<AnnotationPage>(json)
                    Either.Right(
                        GetSearchResultPageResult(
                            response = response, annotationPage = annotationPage
                        )
                    )
                })
        )

    /**
     * Get search status
     *
     * @param queryId
     * @return
     */
    fun getGlobalSearchStatus(queryId: String): Either<RequestError, ARResult.GetGlobalSearchStatusResult> =
        doGet(
            request = webTarget.path(GLOBAL_SERVICES).path(SEARCH).path(queryId).path(STATUS)
                .request(),
            responseHandlers = mapOf(Response.Status.OK to { response ->
                val json = response.readEntityAsJsonString()
                val searchStatus: SearchStatusSummary = oMapper.readValue(json)
                Either.Right(
                    ARResult.GetGlobalSearchStatusResult(response, searchStatus)
                )
            })
        )

    /**
     * Add multi field index
     *
     * @param containerName
     * @param indexDefinition
     * @return
     */
    fun addIndex(
        containerName: String,
        indexDefinition: Map<String, IndexType>
    ): Either<RequestError, AddIndexResult> =
        doPost(
            request = webTarget.path(CONTAINER_SERVICES).path(containerName).path(INDEXES)
                .request(),
            entity = Entity.json(indexDefinition),
            responseHandlers = mapOf(Response.Status.CREATED to { response ->
                val json = response.readEntityAsJsonString()
                val statusSummary: ChoreStatusSummary = oMapper.readValue(json)
                Either.Right(AddIndexResult(response = response, status = statusSummary, indexId = response.indexId()))
            })
        )

    /**
     * add index and (asynchronously) wait for the indexing to be done
     *
     * @param containerName String
     * @param indexDefinition Map<String, IndexType>
     * @return String The index id
     */
    suspend fun asyncAddIndex(
        containerName: String,
        indexDefinition: Map<String, IndexType>
    ): Either<RequestError, String> {
        return either {
            val addingResult = addIndex(containerName, indexDefinition).bind()
            var done = false
            while (!done) {
                val statusResult = getIndexCreationStatus(containerName, addingResult.indexId).bind()
                done = statusResult.status.state != "RUNNING"
                delay(1000)
            }
            addingResult.indexId
        }
    }

//    /**
//     * Get index
//     *
//     * @param containerName
//     * @param fieldName
//     * @param indexType
//     * @return
//     */
//    @Deprecated(message = "Use getIndex()")
//    fun getIndexOld(
//        containerName: String,
//        fieldName: String,
//        indexType: IndexType
//    ): Either<RequestError, GetIndexResult> =
//        doGet(
//            request = webTarget.path(CONTAINER_SERVICES).path(containerName).path(INDEXES).path(fieldName)
//                .path(indexType.name)
//                .request(),
//            responseHandlers = mapOf(Response.Status.OK to { response ->
//                val json = response.readEntityAsJsonString()
//                val indexConfig: IndexConfig = oMapper.readValue(json)
//                Either.Right(
//                    GetIndexResult(response, indexConfig)
//                )
//            })
//        )

    /**
     * Get index
     *
     * @param containerName
     * @param fieldName
     * @param indexType
     * @return
     */
    fun getIndex(containerName: String, indexId: String): Either<RequestError, GetIndexResult> =
        doGet(
            request = webTarget.path(CONTAINER_SERVICES).path(containerName).path(INDEXES).path(indexId)
                .request(),
            responseHandlers = mapOf(Response.Status.OK to { response ->
                val json = response.readEntityAsJsonString()
                val indexConfig: IndexConfig = oMapper.readValue(json)
                Either.Right(
                    GetIndexResult(response, indexConfig)
                )
            })
        )

//    /**
//     * Get index Creation Status
//     *
//     * @param containerName
//     * @param fieldName
//     * @param indexType
//     * @return
//     */
//    fun getIndexCreationStatus(
//        containerName: String,
//        fieldName: String,
//        indexType: IndexType
//    ): Either<RequestError, GetIndexCreationStatusResult> =
//        doGet(
//            request = webTarget.path(CONTAINER_SERVICES).path(containerName).path(INDEXES).path(fieldName)
//                .path(indexType.name).path(STATUS)
//                .request(),
//            responseHandlers = mapOf(Response.Status.OK to { response ->
//                val json = response.readEntityAsJsonString()
//                val statusSummary: ChoreStatusSummary = oMapper.readValue(json)
//                Either.Right(
//                    GetIndexCreationStatusResult(response, statusSummary)
//                )
//            })
//        )

    /**
     * Get index Creation Status
     *
     * @param containerName
     * @param indexId
     * @return
     */
    fun getIndexCreationStatus(
        containerName: String,
        indexId: String
    ): Either<RequestError, GetIndexCreationStatusResult> =
        doGet(
            request = webTarget.path(CONTAINER_SERVICES).path(containerName).path(INDEXES).path(indexId).path(STATUS)
                .request(),
            responseHandlers = mapOf(Response.Status.OK to { response ->
                val json = response.readEntityAsJsonString()
                val statusSummary: ChoreStatusSummary = oMapper.readValue(json)
                Either.Right(
                    GetIndexCreationStatusResult(response, statusSummary)
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
     * @param indexId the index id
     * @return
     */
    fun deleteIndex(
        containerName: String, indexId: String,
    ): Either<RequestError, DeleteResult> = doDelete(
        request = webTarget.path(CONTAINER_SERVICES).path(containerName).path(INDEXES).path(indexId).request(),
        responseHandlers = mapOf(
            Response.Status.NO_CONTENT to { response ->
                Either.Right(DeleteResult(response))
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
    fun addUsers(users: List<UserEntry>): Either<RequestError, AddUsersResult> =
        doPost(
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
    fun deleteUser(userName: String): Either<RequestError, DeleteResult> = doDelete(
        request = webTarget.path(ADMIN).path(USERS).path(userName).request(),
        responseHandlers = mapOf(Response.Status.NO_CONTENT to { response ->
            Either.Right(
                DeleteResult(response)
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
    ): Either<RequestError, DeleteResult> = doDelete(
        request = webTarget.path(CONTAINER_SERVICES).path(containerName).path(USERS).path(userName).request(),
        responseHandlers = mapOf(
            Response.Status.OK to { response ->
                Either.Right(
                    DeleteResult(response = response)
                )
            })
    )

    /**
     * Get a list of containers the user has access to, grouped by access level
     *
     * @return Either<RequestError, MyContainersResult>
     */
    fun getMyContainers(): Either<RequestError, MyContainersResult> = doGet(
        request = webTarget.path(MY).path(CONTAINERS).request(),
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

    /**
     * Get the user profile
     *
     * @return Either<RequestError, MyProfileResult>
     */
    fun getMyProfile(): Either<RequestError, MyProfileResult> = doGet(
        request = webTarget.path(MY).path(PROFILE).request(),
        responseHandlers = mapOf(
            Response.Status.OK to { response ->
                val json = response.readEntityAsJsonString()
                Either.Right(
                    MyProfileResult(
                        response = response,
                        profile = oMapper.readValue(json, object : TypeReference<Map<String, Any>>() {})
                    )
                )
            })
    )

    /**
     * Create a custom query
     *
     * @param name
     * @param queryTemplate
     * @param label
     * @param description
     * @param public
     * @return Either<RequestError, CreateCustomQueryResult>
     */
    fun createCustomQuery(
        name: String,
        queryTemplate: Map<String, Any>,
        label: String = "",
        description: String = "",
        public: Boolean = true
    ): Either<RequestError, CreateCustomQueryResult> = doPost(
        request = webTarget.path(GLOBAL_SERVICES).path(CUSTOM_QUERY).request(),
        entity = Entity.json(
            CustomQuerySpecs(
                name = name,
                query = queryTemplate,
                label = label,
                description = description,
                public = public
            )
//            mapOf(
//                "name" to name,
//                "query" to queryTemplate,
//                "label" to label,
//                "description" to description,
//                "public" to public
//            )
        ),
        responseHandlers = mapOf(
            Response.Status.CREATED to { response ->
                Either.Right(CreateCustomQueryResult(response = response, location = response.location()))
            })
    )

    /**
     * Read a custom query with the variables expanded
     *
     * @param name
     * @param parameters
     * @return Either<RequestError, CreateCustomQueryResult>
     */
    fun readExpandedCustomQuery(name: String, parameters: Map<String, String>): Either<RequestError, QueryAsMap> {
        val queryCall = queryCall(name, parameters)
        return doGet(
            request = webTarget.path(GLOBAL_SERVICES).path(CUSTOM_QUERY).path(queryCall).path(EXPAND).request(),
            responseHandlers = mapOf(
                Response.Status.OK to { response ->
                    val json = response.readEntityAsJsonString()
                    val expandedQuery = oMapper.readValue(json, object : TypeReference<QueryAsMap>() {})
                    Either.Right(expandedQuery)
                })
        )
    }

    /**
     * Delete a custom query
     *
     * @param name
     */
    fun deleteCustomQuery(name: String): Either<RequestError, DeleteResult> = doDelete(
        request = webTarget.path(GLOBAL_SERVICES).path(CUSTOM_QUERY).path(name).request(),
        responseHandlers = mapOf(
            Response.Status.NO_CONTENT to { response ->
                logger.info("$response")
                Either.Right(
                    DeleteResult(response = response)
                )
            })
    )

    fun getCustomQueryResultPage(
        containerName: String,
        name: String,
        parameters: Map<String, String>,
        page: Int = 0
    ): Either<RequestError, GetSearchResultPageResult> = doGet(
        request = webTarget.path(CONTAINER_SERVICES).path(containerName).path(CUSTOM_QUERY)
            .path(queryCall(name, parameters)).queryParam("page", page).request(),
        responseHandlers = mapOf(
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

    fun getCustomQueryResultSequence(
        containerName: String,
        name: String,
        parameters: Map<String, String>
    ): Sequence<Either<RequestError, WebAnnotationAsMap>> = sequence {
        var page = 0
        var goOn = true
        while (goOn) {
            goOn = getCustomQueryResultPage(containerName, name, parameters, page)
                .fold(
                    { error ->
                        yield(Either.Left(error))
                        false
                    },
                    { result ->
                        yieldAll(result.annotationPage.items.map {
                            Either.Right(it)
                        })
                        result.annotationPage.next != null
                    }
                )
            page += 1
        }
    }

    fun containerAdapter(containerName: String): ContainerAdapter = ContainerAdapter(this, containerName)

    data class ContainerAdapter(val client: AnnoRepoClient, val containerName: String) {
        fun create(
            label: String = "",
            readOnlyForAnonymousUsers: Boolean = false
        ): Either<RequestError, CreateContainerResult> =
            client.createContainer(containerName, label = label, readOnlyForAnonymousUsers = readOnlyForAnonymousUsers)

        fun get(): Either<RequestError, GetContainerResult> =
            client.getContainer(containerName)

        fun delete(eTag: String, force: Boolean = false): Either<RequestError, DeleteResult> =
            client.deleteContainer(containerName, eTag = eTag, force = force)

        fun getMetadata(): Either<RequestError, GetContainerMetadataResult> =
            client.getContainerMetadata(containerName)

        fun addAnnotation(
            annotation: WebAnnotationAsMap,
            name: String? = null
        ): Either<RequestError, CreateAnnotationResult> =
            client.createAnnotation(containerName, annotation = annotation, preferredAnnotationName = name)

        fun addAnnotations(annotationList: List<WebAnnotationAsMap>): Either<RequestError, BatchUploadResult> =
            client.batchUpload(containerName, annotations = annotationList)

        fun getAnnotation(name: String): Either<RequestError, GetAnnotationResult> =
            client.getAnnotation(containerName, annotationName = name)

        fun updateAnnotation(
            name: String,
            eTag: String,
            content: WebAnnotationAsMap
        ): Either<RequestError, CreateAnnotationResult> =
            client.updateAnnotation(containerName, annotationName = name, eTag = eTag, annotation = content)

        fun deleteAnnotation(name: String, eTag: String): Either<RequestError, DeleteResult> =
            client.deleteAnnotation(containerName, annotationName = name, eTag = eTag)

        fun createSearch(query: QueryAsMap): Either<RequestError, CreateSearchResult> =
            client.createSearch(containerName, query = query)

        fun getSearchResultPage(queryId: String, page: Int = 0): Either<RequestError, GetSearchResultPageResult> =
            client.getSearchResultPage(containerName, queryId = queryId, page = page)

        fun filterContainerAnnotations(query: QueryAsMap): Either<RequestError, FilterContainerAnnotationsResult> =
            client.filterContainerAnnotations(containerName, query = query)

        fun getSearchInfo(queryId: String): Either<RequestError, GetSearchInfoResult> =
            client.getSearchInfo(containerName, queryId = queryId)

        fun addIndex(indexDefinition: Map<String, IndexType>): Either<RequestError, AddIndexResult> =
            client.addIndex(containerName, indexDefinition)

        suspend fun asyncAddIndex(indexDefinition: Map<String, IndexType>): Either<RequestError, String> =
            client.asyncAddIndex(containerName, indexDefinition)

        fun getIndex(indexId: String): Either<RequestError, GetIndexResult> =
            client.getIndex(containerName, indexId)

        fun listIndexes(): Either<RequestError, ListIndexesResult> =
            client.listIndexes(containerName)

        fun getIndexCreationStatus(
            indexId: String
        ): Either<RequestError, GetIndexCreationStatusResult> =
            client.getIndexCreationStatus(containerName, indexId = indexId)

        fun deleteIndex(indexId: String): Either<RequestError, DeleteResult> =
            client.deleteIndex(containerName, indexId)

        fun getDistinctFieldValues(field: String): Either<RequestError, DistinctAnnotationFieldValuesResult> =
            client.getDistinctFieldValues(containerName, fieldName = field)

        fun setAnonymousUserReadAccess(hasReadAccess: Boolean = true): Either<RequestError, SetAnonymousUserReadAccessResult> =
            client.setAnonymousUserReadAccess(containerName, readOnlyAccess = hasReadAccess)

        fun getCustomQueryResultPage(
            name: String,
            parameters: Map<String, String>
        ): Either<RequestError, GetSearchResultPageResult> =
            client.getCustomQueryResultPage(containerName, name = name, parameters = parameters)

        fun getCustomQueryResultSequence(
            name: String,
            parameters: Map<String, String>
        ): Sequence<Either<RequestError, WebAnnotationAsMap>> =
            client.getCustomQueryResultSequence(containerName, name = name, parameters = parameters)

    }

    suspend fun <R> usingGrpc(block: suspend (AnnoRepoGrpcClient) -> R): R {
        if (apiKey == null) {
            throw RuntimeException("apiKey == null")
        }
        val channel: ManagedChannel = ManagedChannelBuilder
            .forAddress(grpcHost, grpcPort!!)
            .usePlaintext()
            .build()
        return AnnoRepoGrpcClient(channel, apiKey)
            .use { client -> block(client) }
    }

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

    private fun containerSpecs(label: String, readOnlyForAnonymousUsers: Boolean) = mapOf(
        "@context" to listOf(
            "http://www.w3.org/ns/anno.jsonld", "http://www.w3.org/ns/ldp.jsonld"
        ),
        "type" to listOf(
            "BasicContainer", "AnnotationCollection"
        ),
        "label" to label,
        "readOnlyForAnonymousUsers" to readOnlyForAnonymousUsers

    )

    @OptIn(ExperimentalEncodingApi::class)
    private fun queryCall(name: String, parameters: Map<String, String>? = null): String =
        if (parameters == null) {
            name
        } else {
            val encodedParameters = parameters.map { (k, v) -> "$k=${encode(v.encodeToByteArray())}" }.joinToString(",")
            "$name:$encodedParameters"
        }

    private fun Response.indexId(): String =
        links.first { it.rel == "status" }.uri.path.replace("/status", "").split("/").last()

    companion object {
        private val oMapper: ObjectMapper = jacksonObjectMapper()
        private const val PROPERTY_FILE = "annorepo-client.properties"
        const val TWO_HUNDRED_MB = 200 * 1024 * 1024

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
                    logger.warn {
                        "The server at $serverURI has authentication enabled," +
                                " and you did not provide an apiKey." +
                                " You'll only be able to access the endpoints that don't require authentication."
                    }
                }
                annoRepoClient
            } catch (e: RuntimeException) {
                null
            }

    }

}