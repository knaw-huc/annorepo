package nl.knaw.huc.annorepo.resources

import com.codahale.metrics.annotation.Timed
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.google.common.collect.SortedMultiset
import com.google.common.collect.TreeMultiset
import com.mongodb.client.MongoClient
import com.mongodb.client.model.Aggregates
import com.mongodb.client.model.Aggregates.limit
import com.mongodb.client.model.Filters.eq
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import nl.knaw.huc.annorepo.api.ARConst
import nl.knaw.huc.annorepo.api.AnnotationPage
import nl.knaw.huc.annorepo.api.ContainerMetadata
import nl.knaw.huc.annorepo.api.ResourcePaths.SERVICES
import nl.knaw.huc.annorepo.config.AnnoRepoConfiguration
import nl.knaw.huc.annorepo.resources.tools.AggregateStageGenerator
import nl.knaw.huc.annorepo.resources.tools.AnnotationList
import nl.knaw.huc.annorepo.resources.tools.QueryCacheItem
import nl.knaw.huc.annorepo.service.JsonLdUtils.extractFields
import nl.knaw.huc.annorepo.service.UriFactory
import org.bson.Document
import org.eclipse.jetty.util.ajax.JSON
import org.litote.kmongo.findOne
import org.litote.kmongo.getCollection
import org.litote.kmongo.json
import org.slf4j.LoggerFactory
import java.net.URI
import java.util.*
import java.util.concurrent.TimeUnit
import javax.annotation.security.PermitAll
import javax.ws.rs.BadRequestException
import javax.ws.rs.GET
import javax.ws.rs.NotFoundException
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.Context
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.SecurityContext
import javax.ws.rs.core.UriBuilder

@Path(SERVICES)
@Produces(MediaType.APPLICATION_JSON)
@PermitAll
@SecurityRequirement(name = "bearerAuth")
class ServiceResource(
    private val configuration: AnnoRepoConfiguration,
    client: MongoClient
) {
    private val uriFactory = UriFactory(configuration)
    private val mdb = client.getDatabase(configuration.databaseName)

    private val paginationStage = limit(configuration.pageSize)
    private val aggregateStageGenerator = AggregateStageGenerator(configuration)

    private val queryCache: Cache<String, QueryCacheItem> = Caffeine.newBuilder()
        .expireAfterAccess(1, TimeUnit.HOURS)
        .maximumSize(1000)
        .build()
    private val log = LoggerFactory.getLogger(javaClass)

    @Operation(description = "Find annotations in the given container matching the given query")
    @Timed
    @POST
    @Path("{containerName}/search")
    fun createSearch(
        @PathParam("containerName") containerName: String,
        queryJson: String,
        @Context context: SecurityContext
    ): Response {
        checkContainerExists(containerName)
        val queryMap = JSON.parse(queryJson)
        if (queryMap is HashMap<*, *>) {
            val aggregateStages = queryMap.toMap()
                .map { (k, v) -> aggregateStageGenerator.generateStage(k, v) }
                .toList()
            val container = mdb.getCollection(containerName)
            val count = container.aggregate(aggregateStages).count()

            val id = UUID.randomUUID().toString()
            queryCache.put(id, QueryCacheItem(queryMap, aggregateStages, count))
            val location = uriFactory.searchURL(containerName, id)
            return Response.created(location)
                .link(uriFactory.searchInfoURL(containerName, id), "info")
                .entity(mapOf("hits" to count))
                .build()
        }
        return Response.status(Response.Status.BAD_REQUEST).build()
    }

    @Operation(description = "Get the given search result page")
    @Timed
    @GET
    @Path("{containerName}/search/{searchId}")
    fun getSearchResultPage(
        @PathParam("containerName") containerName: String,
        @PathParam("searchId") searchId: String,
        @QueryParam("page") page: Int = 0,
        @Context context: SecurityContext
    ): Response {
        checkContainerExists(containerName)

        val queryCacheItem = getQueryCacheItem(searchId)
        val aggregateStages = queryCacheItem.aggregateStages.toMutableList().apply {
            add(Aggregates.skip(page * configuration.pageSize))
            add(paginationStage)
        }
//        log.debug("aggregateStages=\n  {}", Joiner.on("\n  ").join(aggregateStages))

        val annotations =
            mdb.getCollection(containerName)
                .aggregate(aggregateStages)
                .map { a -> toAnnotationMap(a, containerName) }
                .toList()
        val annotationPage =
            buildAnnotationPage(uriFactory.searchURL(containerName, searchId), annotations, page, queryCacheItem.count)
        return Response.ok(annotationPage).build()
    }

    @Operation(description = "Get information about the given search")
    @Timed
    @GET
    @Path("{containerName}/search/{searchId}/info")
    fun getSearchInfo(
        @PathParam("containerName") containerName: String,
        @PathParam("searchId") searchId: String,
        @Context context: SecurityContext
    ): Response {
        checkContainerExists(containerName)
        val queryCacheItem = getQueryCacheItem(searchId)
        val info = mapOf("query" to queryCacheItem.queryMap, "hits" to queryCacheItem.count)
        return Response.ok(info).build()
    }

    @Operation(description = "Get a list of the fields used in the annotations in a container")
    @Timed
    @GET
    @Path("{containerName}/fields")
    fun getAnnotationFieldsForContainer(
        @PathParam("containerName") containerName: String
    ): Response {
        checkContainerExists(containerName)
        val container = mdb.getCollection(containerName)
        val fields = container.find()
            .flatMap { d -> extractFields(d["annotation"]!!.json) }
            .filter { f -> !f.contains("@") }
            .toList()
        val bag: SortedMultiset<String> = TreeMultiset.create()
        for (f in fields) {
            bag.add(f)
        }
        val fieldCounts = mutableMapOf<String, Int>()
        bag.forEachEntry { field, count -> fieldCounts[field] = count }
        return Response.ok(fieldCounts).build()
    }

    @Operation(description = "Get some container metadata")
    @Timed
    @GET
    @Path("{containerName}/metadata")
    fun getMetadataForContainer(
        @PathParam("containerName") containerName: String
    ): Response {
        checkContainerExists(containerName)
        val container = mdb.getCollection(containerName)
        val containerMetadataStore = mdb.getCollection<ContainerMetadata>(ARConst.CONTAINER_METADATA_COLLECTION)
        val meta = containerMetadataStore.findOne { eq("name", containerName) }!!

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

    private fun getQueryCacheItem(searchId: String): QueryCacheItem =
        queryCache.getIfPresent(searchId)
            ?: throw NotFoundException("No search results found for this search id. The search might have expired.")

    private fun buildAnnotationPage(
        searchUri: URI,
        annotations: AnnotationList,
        page: Int,
        total: Int
    ): AnnotationPage {
        val prevPage = if (page > 0) {
            page - 1
        } else {
            null
        }
        val startIndex = configuration.pageSize * page
        val nextPage = if (startIndex + annotations.size < total) {
            page + 1
        } else {
            null
        }

        return AnnotationPage(
            id = searchPageUri(searchUri, page),
            partOf = searchUri.toString(),
            startIndex = startIndex,
            items = annotations,
            prev = if (prevPage != null) searchPageUri(searchUri, prevPage) else null,
            next = if (nextPage != null) searchPageUri(searchUri, nextPage) else null
        )
    }

    private fun checkContainerExists(containerName: String) {
        if (!mdb.listCollectionNames().contains(containerName)) {
            throw BadRequestException("Annotation Container '$containerName' not found")
        }
    }

    private fun searchPageUri(searchUri: URI, page: Int) =
        UriBuilder.fromUri(searchUri)
            .queryParam("page", page)
            .build()
            .toString()

    private fun toAnnotationMap(a: Document, containerName: String): Map<String, Any> =
        a.get("annotation", Document::class.java)
            .toMutableMap()
            .apply<MutableMap<String, Any>> {
                put(
                    "id",
                    uriFactory.annotationURL(containerName, a.getString("annotation_name"))
                )
            }
}

