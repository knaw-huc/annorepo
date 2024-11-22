package nl.knaw.huc.annorepo.resources

import java.io.StringReader
import java.net.URI
import java.util.UUID
import java.util.concurrent.TimeUnit
import jakarta.annotation.security.PermitAll
import jakarta.json.Json
import jakarta.ws.rs.BadRequestException
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.ForbiddenException
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.NotFoundException
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.Context
import jakarta.ws.rs.core.MediaType.APPLICATION_JSON
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.core.SecurityContext
import jakarta.ws.rs.core.UriBuilder
import com.codahale.metrics.annotation.Timed
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import com.google.common.cache.RemovalListener
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoCursor
import com.mongodb.client.model.Aggregates
import com.mongodb.client.model.Aggregates.limit
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import org.apache.logging.log4j.kotlin.logger
import org.bson.Document
import org.bson.conversions.Bson
import nl.knaw.huc.annorepo.api.ANNO_JSONLD_URL
import nl.knaw.huc.annorepo.api.ARConst.ANNOTATION_FIELD
import nl.knaw.huc.annorepo.api.ARConst.ANNOTATION_NAME_FIELD
import nl.knaw.huc.annorepo.api.ARConst.SECURITY_SCHEME_NAME
import nl.knaw.huc.annorepo.api.AnnotationIdentifier
import nl.knaw.huc.annorepo.api.AnnotationPage
import nl.knaw.huc.annorepo.api.ContainerMetadata
import nl.knaw.huc.annorepo.api.ContainerUserEntry
import nl.knaw.huc.annorepo.api.IndexConfig
import nl.knaw.huc.annorepo.api.IndexType
import nl.knaw.huc.annorepo.api.OldAnnotationPage
import nl.knaw.huc.annorepo.api.QueryAsMap
import nl.knaw.huc.annorepo.api.ResourcePaths.ANNOTATIONS_BATCH
import nl.knaw.huc.annorepo.api.ResourcePaths.COLLECTION
import nl.knaw.huc.annorepo.api.ResourcePaths.CONTAINER_SERVICES
import nl.knaw.huc.annorepo.api.ResourcePaths.CUSTOM_QUERY
import nl.knaw.huc.annorepo.api.ResourcePaths.DISTINCT_FIELD_VALUES
import nl.knaw.huc.annorepo.api.ResourcePaths.FIELDS
import nl.knaw.huc.annorepo.api.ResourcePaths.INDEXES
import nl.knaw.huc.annorepo.api.ResourcePaths.INFO
import nl.knaw.huc.annorepo.api.ResourcePaths.METADATA
import nl.knaw.huc.annorepo.api.ResourcePaths.READ_ONLY_FOR_ANONYMOUS
import nl.knaw.huc.annorepo.api.ResourcePaths.SEARCH
import nl.knaw.huc.annorepo.api.ResourcePaths.SETTINGS
import nl.knaw.huc.annorepo.api.ResourcePaths.USERS
import nl.knaw.huc.annorepo.api.SearchInfo
import nl.knaw.huc.annorepo.api.WebAnnotationAsMap
import nl.knaw.huc.annorepo.config.AnnoRepoConfiguration
import nl.knaw.huc.annorepo.dao.ContainerDAO
import nl.knaw.huc.annorepo.dao.ContainerUserDAO
import nl.knaw.huc.annorepo.dao.CustomQueryDAO
import nl.knaw.huc.annorepo.resources.tools.AggregateStageGenerator
import nl.knaw.huc.annorepo.resources.tools.AnnotationList
import nl.knaw.huc.annorepo.resources.tools.ContainerAccessChecker
import nl.knaw.huc.annorepo.resources.tools.CustomQueryTools
import nl.knaw.huc.annorepo.resources.tools.CustomQueryTools.interpolate
import nl.knaw.huc.annorepo.resources.tools.IndexManager
import nl.knaw.huc.annorepo.resources.tools.QueryCacheItem
import nl.knaw.huc.annorepo.resources.tools.annotationCollectionLink
import nl.knaw.huc.annorepo.resources.tools.isOpenAndHasNext
import nl.knaw.huc.annorepo.resources.tools.simplify
import nl.knaw.huc.annorepo.service.UriFactory

@Path(CONTAINER_SERVICES)
@Produces(APPLICATION_JSON)
@PermitAll
@SecurityRequirement(name = SECURITY_SCHEME_NAME)
class ContainerServiceResource(
    private val configuration: AnnoRepoConfiguration,
    private val containerUserDAO: ContainerUserDAO,
    private val containerDAO: ContainerDAO,
    private val customQueryDAO: CustomQueryDAO,
    private val uriFactory: UriFactory,
    private val indexManager: IndexManager
) : AbstractContainerResource(configuration, containerDAO, ContainerAccessChecker(containerUserDAO)) {

    private val paginationStage = limit(configuration.pageSize)
    private val aggregateStageGenerator = AggregateStageGenerator(configuration)

    private val queryCache: LoadingCache<String, QueryCacheItem> = CacheBuilder.newBuilder()
        .maximumSize(1000)
        .expireAfterAccess(1, TimeUnit.HOURS)
        .build(CacheLoader.from { _: String -> null })

    private val mongoCursorRemovalListener =
        RemovalListener<String, MongoCursor<Document>> { removal ->
            val cursor = removal.value
            logger.debug { "removing ${removal.key} from cache" }
            logger.debug { "closing cursor ${removal.key}" }
            cursor?.close()
        }
    private val mongoCursorCache: LoadingCache<String, MongoCursor<Document>> = CacheBuilder.newBuilder()
        .maximumSize(1000)
        .expireAfterAccess(1, TimeUnit.HOURS)
        .removalListener(mongoCursorRemovalListener)
        .build(CacheLoader.from { _: String -> null })

    @Operation(description = "Turn read-only access to this container for anonymous users on or off")
    @Timed
    @PUT
    @Path("{containerName}/$SETTINGS/$READ_ONLY_FOR_ANONYMOUS")
    fun setAnonymousUserReadAccess(
        @PathParam("containerName") containerName: String,
        setting: Boolean,
        @Context context: SecurityContext,
    ): Response {
        context.checkUserHasAdminRightsInThisContainer(containerName)
        val containerMetadata: ContainerMetadata =
            containerDAO.getContainerMetadata(containerName)!!
        val newContainerMetadata = containerMetadata.copy(isReadOnlyForAnonymous = setting)
        containerDAO.updateContainerMetadata(containerName, newContainerMetadata, false)
        return Response.ok().build()
    }

    @Operation(description = "Show the users with access to this container")
    @Timed
    @GET
    @Path("{containerName}/$USERS")
    fun readContainerUsers(
        @PathParam("containerName") containerName: String,
        @Context context: SecurityContext,
    ): Response {
        context.checkUserHasAdminRightsInThisContainer(containerName)

        val users = containerUserDAO.getUsersForContainer(containerName)
        return Response.ok(users).build()
    }

    @Operation(description = "Add users with given role to this container")
    @Timed
    @POST
    @Path("{containerName}/$USERS")
    @Consumes(APPLICATION_JSON)
    fun addContainerUsers(
        @PathParam("containerName") containerName: String,
        @Context context: SecurityContext,
        containerUsers: List<ContainerUserEntry>,
    ): Response {
        context.checkUserHasAdminRightsInThisContainer(containerName)

        for (user in containerUsers) {
            containerUserDAO.removeContainerUser(containerName, user.userName)
            containerUserDAO.addContainerUser(containerName, user.userName, user.role)
        }
        val users = containerUserDAO.getUsersForContainer(containerName)
        return Response.ok(users).build()
    }

    @Operation(description = "Remove the user with the given userName from this container")
    @Timed
    @DELETE
    @Path("{containerName}/$USERS/{userName}")
    fun deleteContainerUser(
        @PathParam("containerName") containerName: String,
        @PathParam("userName") userName: String,
        @Context context: SecurityContext,
    ): Response {
        context.checkUserHasAdminRightsInThisContainer(containerName)
        containerUserDAO.removeContainerUser(containerName, userName)
        return Response.ok().build()
    }

    @Operation(description = "Find annotations in the given container matching the given query")
    @Timed
    @POST
    @Path("{containerName}/$SEARCH")
    @Consumes(APPLICATION_JSON)
    fun createSearch(
        @PathParam("containerName") containerName: String,
        queryJson: String,
        @Context context: SecurityContext,
    ): Response {
        context.checkUserHasReadRightsInThisContainer(containerName)
        try {
            val queryMap: QueryAsMap = Json.createReader(StringReader(queryJson)).readObject().toMap().simplify()
            val aggregateStages = queryMap
                .map { (k, v) -> aggregateStageGenerator.generateStage(k, v) }
                .toList()

            val id = UUID.randomUUID().toString()
            queryCache.put(id, QueryCacheItem(queryMap, aggregateStages, -1))
            logger.debug { "explain aggregate =\n\n${asMongoExplain(containerName, aggregateStages)}\n" }
            val location = uriFactory.searchURL(containerName, id)
            return Response.created(location)
                .link(uriFactory.searchInfoURL(containerName, id), "info")
                .build()
        } catch (e: RuntimeException) {
            e.printStackTrace()
            return Response.status(Response.Status.BAD_REQUEST).build()
        }
    }

    @Operation(description = "Get the given search result page")
    @Timed
    @GET
    @Path("{containerName}/$SEARCH/{searchId}")
    fun getSearchResultPage(
        @PathParam("containerName") containerName: String,
        @PathParam("searchId") searchId: String,
        @QueryParam("page") page: Int = 0,
        @HeaderParam("User-Agent") userAgent: String,
        @Context context: SecurityContext,
    ): Response {
        context.checkUserHasReadRightsInThisContainer(containerName)

        val queryCacheItem = getQueryCacheItem(searchId)
        val cacheKey = "$searchId:${page}"
        val cursor = mongoCursorCache.getIfPresent(cacheKey)?.also {
            logger.debug { "using cached cursor $cacheKey" }
        } ?: createContainerSearchCursor(containerName, queryCacheItem, page)

        val annotations = mutableListOf<WebAnnotationAsMap>()
        while (annotations.size < configuration.pageSize && cursor.isOpenAndHasNext()) {
            annotations.add(cursor.next().toAnnotationMap(containerName))
        }

        val hasNext = cursor.isOpenAndHasNext()
        if (hasNext) {
            val nextCacheKey = "$searchId:${page + 1}"
            logger.debug { "storing cursor $nextCacheKey" }
            mongoCursorCache.put(nextCacheKey, cursor)
        } else {
            cursor.close()
        }
        mongoCursorCache.invalidate(cacheKey)

        val useOld = userAgent.contains("AnnoRepoClient/0.6")
        if (useOld) {
            val annotationPage = buildOldAnnotationPage(
                uriFactory.searchURL(containerName, searchId),
                annotations,
                page,
                hasNext = annotations.size == configuration.pageSize
            )
            return Response.ok(annotationPage).build()
        } else {
            val annotationPage = buildAnnotationPage(
                uriFactory.searchURL(containerName, searchId),
                annotations,
                page,
                hasNext = annotations.size == configuration.pageSize
            )
            return Response.ok(annotationPage).build()
        }

    }

//    @Operation(description = "Get the given search result page")
//    @Timed
//    @GET
//    @Path("{containerName}/o$SEARCH/{searchId}")
//    fun getSearchResultPage(
//        @PathParam("containerName") containerName: String,
//        @PathParam("searchId") searchId: String,
//        @QueryParam("page") page: Int = 0,
//        @Context context: SecurityContext,
//    ): Response {
//        context.checkUserHasReadRightsInThisContainer(containerName)
//
//        var queryCacheItem = getQueryCacheItem(searchId)
//        if (queryCacheItem.count < 1) {
////            val count = containerDAO.getCollection(containerName)
////                .aggregate(queryCacheItem.aggregateStages)
////                .count()
//            val newQueryCacheItem = QueryCacheItem(queryCacheItem.queryMap, queryCacheItem.aggregateStages, 1)
//            queryCacheItem = newQueryCacheItem
//            queryCache.put(searchId, newQueryCacheItem)
//        }
//        val aggregateStages = queryCacheItem.aggregateStages.toMutableList().apply {
//            add(Aggregates.skip(page * configuration.pageSize))
//            add(paginationStage)
//        }
////        log.debug("aggregateStages=\n  {}", Joiner.on("\n  ").join(aggregateStages))
//
//        val annotations =
//            containerDAO.getCollection(containerName)
//                .aggregate(aggregateStages)
//                .map { a -> a.toAnnotationMap(containerName) }
//                .toList()
//        val annotationPage =
//            buildAnnotationPage(
//                uriFactory.searchURL(containerName, searchId),
//                annotations,
//                page,
//                hasNext = annotations.size == configuration.pageSize
//            )
//        return Response.ok(annotationPage).build()
//    }

    @Operation(description = "Get information about the given search")
    @Timed
    @GET
    @Path("{containerName}/$SEARCH/{searchId}/$INFO")
    fun getSearchInfo(
        @PathParam("containerName") containerName: String,
        @PathParam("searchId") searchId: String,
        @Context context: SecurityContext,
    ): Response {
        context.checkUserHasReadRightsInThisContainer(containerName)

        val queryCacheItem = getQueryCacheItem(searchId)
//        val info = mapOf("query" to queryCacheItem.queryMap, "hits" to queryCacheItem.count)
        val query = queryCacheItem.queryMap
        val searchInfo = SearchInfo(
            query = query,
            hits = queryCacheItem.count
        )
        return Response.ok(searchInfo).build()
    }

    @Operation(description = "Get the results of the given custom query")
    @Timed
    @GET
    @Path("{containerName}/${CUSTOM_QUERY}/{queryCall}")
    fun getCustomQueryResultPage(
        @PathParam("containerName") containerName: String,
        @PathParam("queryCall") queryCall: String,
        @QueryParam("page") page: Int = 0,
        @Context context: SecurityContext,
    ): Response {
        context.checkUserHasReadRightsInThisContainer(containerName)
        val cacheKey = "$containerName:$queryCall:${page}"
        val cursor = mongoCursorCache.getIfPresent(cacheKey)?.also {
            logger.debug { "using cached cursor $cacheKey" }
        } ?: createCustomSearchCursor(context, containerName, queryCall, page)

        val annotations = mutableListOf<WebAnnotationAsMap>()
        while (annotations.size < configuration.pageSize && cursor.isOpenAndHasNext()) {
            annotations.add(cursor.next().toAnnotationMap(containerName))
        }

        val hasNext = cursor.isOpenAndHasNext()
        if (hasNext) {
            val nextCacheKey = "$containerName:$queryCall:${page + 1}"
            logger.debug { "storing cursor $nextCacheKey" }
            mongoCursorCache.put(nextCacheKey, cursor)
        } else {
            cursor.close()
        }
        mongoCursorCache.invalidate(cacheKey)

        val (queryName, queryParameters) = CustomQueryTools.decode(queryCall)
            .getOrElse { throw BadRequestException(it.message) }
        val customQuery = customQueryDAO.getByName(queryName)
            ?: throw NotFoundException("No custom query '$queryName' found")

        val annotationPage =
            buildAnnotationPage(
                uriFactory.customContainerQueryURL(containerName, queryCall),
                annotations,
                page,
                hasNext = hasNext,
                collectionLabel = customQuery.label?.interpolate(queryParameters = queryParameters),
                collectionUrl = uriFactory.customContainerQueryCollectionURL(containerName, queryCall)
            )
        return Response.ok(annotationPage)
            .link(uriFactory.customQueryURL(queryName), "using")
            .link(uriFactory.expandedCustomQueryURL(queryCall), "query")
            .build()
    }

    @Operation(description = "Get the AnnotationCollection of the given custom query")
    @Timed
    @GET
    @Path("{containerName}/${CUSTOM_QUERY}/{queryCall}/$COLLECTION")
    fun getCustomQueryAnnotationCollection(
        @PathParam("containerName") containerName: String,
        @PathParam("queryCall") queryCall: String,
        @Context context: SecurityContext,
    ): Response {
        context.checkUserHasReadRightsInThisContainer(containerName)
        val (queryName, queryParameters) = CustomQueryTools.decode(queryCall)
            .getOrElse { throw BadRequestException(it.message) }
        val customQuery = customQueryDAO.getByName(queryName)
            ?: throw NotFoundException("No custom query '$queryName' found")

        val collection = mapOf(
            "@context" to ANNO_JSONLD_URL,
            "id" to uriFactory.customContainerQueryCollectionURL(containerName, queryCall),
            "type" to "AnnotationCollection",
            "label" to (customQuery.label?.interpolate(queryParameters) ?: ""),
            "creator" to customQuery.createdBy,
//            "total" to total,
            "first" to mapOf(
                "id" to uriFactory.customContainerQueryURL(containerName, queryCall, 0),
                "type" to "AnnotationPage"
            ),
//            "last" to "http://example.org/page42"
        )
        return Response.ok(collection)
            .build()
    }

    @Operation(description = "Get a list of the fields used in the annotations in a container")
    @Timed
    @GET
    @Path("{containerName}/$FIELDS")
    fun getAnnotationFieldsForContainer(
        @PathParam("containerName") containerName: String,
        @Context context: SecurityContext,
    ): Response {
        context.checkUserHasReadRightsInThisContainer(containerName)

        val sortedMap = containerDAO.getAnnotationFields(containerName)
        return Response.ok(sortedMap).build()
    }

    @Operation(description = "Get a list of the fields used in the annotations in a container")
    @Timed
    @GET
    @Path("{containerName}/$DISTINCT_FIELD_VALUES/{field}")
    fun getDistinctAnnotationFieldsValuesForContainer(
        @PathParam("containerName") containerName: String,
        @PathParam("field") field: String,
        @Context context: SecurityContext,
    ): Response {
        context.checkUserHasReadRightsInThisContainer(containerName)
        val distinctValues = containerDAO.getDistinctValues(containerName, field)
        return Response.ok(distinctValues).build()
    }

    @Operation(description = "Get some container metadata")
    @Timed
    @GET
    @Path("{containerName}/$METADATA")
    fun getMetadataForContainer(
        @PathParam("containerName") containerName: String,
        @Context context: SecurityContext,
    ): Response {
        context.checkUserHasReadRightsInThisContainer(containerName)

        val container = containerDAO.getCollection(containerName)
        val meta = containerDAO.getContainerMetadata(containerName)!!

        val metadata = mapOf(
            "id" to uriFactory.containerURL(containerName),
            "label" to meta.label,
            "created" to meta.createdAt,
            "modified" to meta.modifiedAt,
            "size" to container.countDocuments(),
            "indexes" to container.listIndexes(),
        )
        return Response.ok(metadata).build()
    }

    @Operation(description = "List a container's indexes")
    @Timed
    @GET
    @Path("{containerName}/$INDEXES")
    fun getContainerIndexes(
        @PathParam("containerName") containerName: String,
        @Context context: SecurityContext,
    ): Response {
        context.checkUserHasReadRightsInThisContainer(containerName)

        val container = containerDAO.getCollection(containerName)
        val body = indexData(container, containerName)
        return Response.ok(body).build()
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Operation(description = "Add a multi-field index")
    @Timed
    @POST
    @Path("{containerName}/$INDEXES")
    fun addContainerIndex(
        @PathParam("containerName") containerName: String,
        multiFieldIndexSettings: Map<String, String>,
        @Context context: SecurityContext,
    ): Response {
        context.checkUserHasAdminRightsInThisContainer(containerName)
        val indexParts = multiFieldIndexSettings.map { (fieldName, indexTypeParam) ->
            val indexType =
                IndexType.fromString(indexTypeParam) ?: throw BadRequestException(
                    "Unknown indexType $indexTypeParam; expected indexTypes: ${
                        IndexType.entries.joinToString(", ") { it.name.lowercase() }
                    }"
                )
            IndexManager.IndexPart(fieldName, indexTypeParam, indexType)
        }
        val indexChore =
            indexManager.startIndexCreation(containerName, indexParts)
//        val indexName = multiFieldIndexSettings.indexName()
        val indexName = indexChore.id
        val location = uriFactory.containerIndexURL(containerName, indexName)
        return Response.created(location)
            .link(uriFactory.containerIndexStatusURL(containerName, indexName), "status")
            .entity(indexChore.status.summary())
            .build()
    }

    @Operation(description = "Get an index definition")
    @Timed
    @GET
    @Path("{containerName}/$INDEXES/{indexId}")
    fun getContainerIndexDefinition(
        @PathParam("containerName") containerName: String,
        @PathParam("indexId") indexId: String,
        @Context context: SecurityContext,
    ): Response {
        context.checkUserHasAdminRightsInThisContainer(containerName)

        val indexConfig = containerDAO.getContainerIndexDefinition(containerName, indexId)
        return Response.ok(indexConfig).build()
    }

    @Operation(description = "Get an index status")
    @Timed
    @GET
    @Path("{containerName}/$INDEXES/{indexId}/status")
    fun getContainerIndexStatus(
        @PathParam("containerName") containerName: String,
        @PathParam("indexId") indexId: String,
        @Context context: SecurityContext,
    ): Response {
        context.checkUserHasAdminRightsInThisContainer(containerName)

        val indexChore = indexManager.getIndexChore(indexId) ?: throw NotFoundException()
        return Response.ok(indexChore.status.summary()).build()
    }

    @Operation(description = "Delete a container index")
    @Timed
    @DELETE
    @Path("{containerName}/$INDEXES/{indexId}")
    fun deleteContainerIndex(
        @PathParam("containerName") containerName: String,
        @PathParam("indexId") indexId: String,
        @Context context: SecurityContext,
    ): Response {
        context.checkUserHasAdminRightsInThisContainer(containerName)

        containerDAO.dropContainerIndex(containerName, indexId)
        return Response.noContent().build()
    }

    @Operation(description = "Upload annotations in batch to a given container")
    @Timed
    @POST
    @Path("{containerName}/${ANNOTATIONS_BATCH}")
    fun postAnnotationsBatch(
        @PathParam("containerName") containerName: String,
        annotations: List<WebAnnotationAsMap>,
        @Context context: SecurityContext,
    ): Response {
        context.checkUserHasEditRightsInThisContainer(containerName)

        val annotationIdentifiers: List<AnnotationIdentifier> =
            containerDAO.addAnnotationsInBatch(containerName, annotations)
        return Response.ok(annotationIdentifiers).build()
    }

    private fun indexData(container: MongoCollection<Document>, containerName: String): List<IndexConfig> =
        container.listIndexes()
            .filter { it.toMap()["name"].toString().startsWith("$ANNOTATION_FIELD.") }
            .mapNotNull { it.toMap().asIndexConfig(containerName) }
            .map { indexConfig ->
                indexConfig.copy(indexFields = indexConfig.indexFields.map { indexFields ->
                    indexFields.copy(
                        field = indexFields.field.replace(
                            "$ANNOTATION_FIELD.",
                            ""
                        )
                    )
                })
            }
            .toList()

    private fun Map<String, Any>.asIndexConfig(containerName: String): IndexConfig {
        val name = this["name"].toString()
        val indexId =
            containerDAO.getContainerMetadata(containerName)?.indexMap?.filter { it.value == name }?.map { it.key }
                ?.first() ?: throw RuntimeException("No indexId found for index $name in $containerName")
        return containerDAO.indexConfig(containerName, name, indexId)
    }

    private fun getQueryCacheItem(searchId: String): QueryCacheItem =
        queryCache.getIfPresent(searchId)
            ?: throw NotFoundException("No search results found for this search id. The search might have expired.")

    private fun buildAnnotationPage(
        searchUri: URI,
        annotations: AnnotationList,
        page: Int,
        hasNext: Boolean = true,
        collectionLabel: String? = null,
        collectionUrl: URI? = null
    ): AnnotationPage {
        val prevPage = if (page > 0) {
            page - 1
        } else {
            null
        }
        val startIndex = configuration.pageSize * page
        val nextPage = if (hasNext) {
            page + 1
        } else {
            null
        }
        val collectionId = collectionUrl?.toString() ?: searchUri.toString()
        return AnnotationPage(
            context = listOf(ANNO_JSONLD_URL),
            id = searchPageUri(searchUri, page),
            partOf = annotationCollectionLink(id = collectionId, collectionLabel = collectionLabel),
            startIndex = startIndex,
            items = annotations,
            prev = if (prevPage != null) searchPageUri(searchUri, prevPage) else null,
            next = if (nextPage != null) searchPageUri(searchUri, nextPage) else null
        )
    }

    private fun buildOldAnnotationPage(
        searchUri: URI,
        annotations: AnnotationList,
        page: Int,
        hasNext: Boolean = true,
        collectionUrl: URI? = null
    ): OldAnnotationPage {
        val prevPage = if (page > 0) {
            page - 1
        } else {
            null
        }
        val startIndex = configuration.pageSize * page
        val nextPage = if (hasNext) {
            page + 1
        } else {
            null
        }
        val collectionId = collectionUrl?.toString() ?: searchUri.toString()
        return OldAnnotationPage(
            context = listOf(ANNO_JSONLD_URL),
            id = searchPageUri(searchUri, page),
            partOf = collectionId,
            startIndex = startIndex,
            items = annotations,
            prev = if (prevPage != null) searchPageUri(searchUri, prevPage) else null,
            next = if (nextPage != null) searchPageUri(searchUri, nextPage) else null
        )
    }

    private fun searchPageUri(searchUri: URI, page: Int) =
        UriBuilder.fromUri(searchUri).queryParam("page", page).build().toString()

    private fun Document.toAnnotationMap(containerName: String): WebAnnotationAsMap =
        this[ANNOTATION_FIELD, Document::class.java]
            .toMutableMap()
            .apply<MutableMap<String, Any>> {
                put(
                    "id", uriFactory.annotationURL(containerName, getString(ANNOTATION_NAME_FIELD))
                )
            }

    private fun asMongoExplain(containerName: String, aggregateStages: List<Bson>): String {
        val stages = aggregateStages.joinToString(", ") { it.toBsonDocument().toString() }
        return """
            db["$containerName"].aggregate(
                [$stages],
                { explain: true }
            )
        """.trimIndent()
    }

    private fun createCustomSearchCursor(
        context: SecurityContext,
        containerName: String,
        queryCall: String,
        page: Int
    ): MongoCursor<Document> {
        logger.debug { "creating new cursor" }
        val (queryName, queryParameters) = CustomQueryTools.decode(queryCall)
            .getOrElse { throw BadRequestException(it.message) }
        val customQuery = customQueryDAO.getByName(queryName)
            ?: throw NotFoundException("No custom query '$queryName' found")
        if (!customQuery.public && customQuery.createdBy != context.userPrincipal?.name) {
            throw ForbiddenException("Custom query '$queryCall' is not for public use")
        }

        logger.debug { customQuery.queryTemplate }
        val queryJson = customQuery.queryTemplate.interpolate(queryParameters)
        val queryMap: QueryAsMap = Json.createReader(StringReader(queryJson)).readObject().toMap().simplify()
        val aggregateStages = queryMap
            .map { (k, v) -> aggregateStageGenerator.generateStage(k, v) }
            .toList()
            .toMutableList()
            .apply {
                add(Aggregates.skip(page * configuration.pageSize))
            }

        return containerDAO
            .getCollection(containerName)
            .aggregate(aggregateStages)
            .cursor()
    }

    private fun createContainerSearchCursor(
        containerName: String,
        queryCacheItem: QueryCacheItem,
        page: Int
    ): MongoCursor<Document> {
        logger.debug { "creating new cursor" }
        val aggregateStages = queryCacheItem.aggregateStages
            .toMutableList()
            .apply {
                add(Aggregates.skip(page * configuration.pageSize))
            }
        return containerDAO
            .getCollection(containerName)
            .aggregate(aggregateStages)
            .cursor()
    }

}




