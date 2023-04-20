package nl.knaw.huc.annorepo.resources

import java.net.URI
import java.util.*
import java.util.concurrent.TimeUnit
import javax.annotation.security.PermitAll
import javax.ws.rs.*
import javax.ws.rs.core.*
import javax.ws.rs.core.MediaType.APPLICATION_JSON
import com.codahale.metrics.annotation.Timed
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.mongodb.client.MongoClient
import com.mongodb.client.model.Aggregates
import com.mongodb.client.model.Aggregates.limit
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import org.bson.Document
import org.eclipse.jetty.util.ajax.JSON
import org.slf4j.LoggerFactory
import nl.knaw.huc.annorepo.api.*
import nl.knaw.huc.annorepo.api.ARConst.ANNOTATION_FIELD
import nl.knaw.huc.annorepo.api.ARConst.ANNOTATION_NAME_FIELD
import nl.knaw.huc.annorepo.api.ARConst.SECURITY_SCHEME_NAME
import nl.knaw.huc.annorepo.api.ResourcePaths.GLOBAL_SERVICES
import nl.knaw.huc.annorepo.auth.ContainerUserDAO
import nl.knaw.huc.annorepo.config.AnnoRepoConfiguration
import nl.knaw.huc.annorepo.resources.tools.AggregateStageGenerator
import nl.knaw.huc.annorepo.resources.tools.AnnotationList
import nl.knaw.huc.annorepo.resources.tools.ContainerAccessChecker
import nl.knaw.huc.annorepo.resources.tools.QueryCacheItem
import nl.knaw.huc.annorepo.service.UriFactory

@Path(GLOBAL_SERVICES)
@Produces(APPLICATION_JSON)
@PermitAll
@SecurityRequirement(name = SECURITY_SCHEME_NAME)
class GlobalServiceResource(
    private val configuration: AnnoRepoConfiguration,
    client: MongoClient,
    private val containerUserDAO: ContainerUserDAO,
) : AbstractContainerResource(configuration, client, ContainerAccessChecker(containerUserDAO)) {
    private val uriFactory = UriFactory(configuration)

    private val paginationStage = limit(configuration.pageSize)
    private val aggregateStageGenerator = AggregateStageGenerator(configuration)

    private val queryCache: Cache<String, QueryCacheItem> =
        Caffeine.newBuilder().expireAfterAccess(1, TimeUnit.HOURS).maximumSize(1000).build()
    private val log = LoggerFactory.getLogger(javaClass)

    @Operation(description = "Find annotations in accessible containers matching the given query")
    @Timed
    @POST
    @Path("search")
    @Consumes(APPLICATION_JSON)
    fun createSearch(
        queryJson: String,
        @Context context: SecurityContext,
    ): Response {
        val queryMap = JSON.parse(queryJson)
        if (queryMap is HashMap<*, *>) {
            val aggregateStages =
                queryMap.toMap().map { (k, v) -> aggregateStageGenerator.generateStage(k, v) }.toList()
            val id = UUID.randomUUID().toString()
            queryCache.put(id, QueryCacheItem(queryMap, aggregateStages, 0))
            val location = uriFactory.globalSearchURL(id)
            return Response.created(location)
                .link(uriFactory.globalSearchInfoURL(id), "info")
                .build()
        }
        return Response.status(Response.Status.BAD_REQUEST).build()
    }

    @Operation(description = "Get the given global search result page")
    @Timed
    @GET
    @Path("search/{searchId}")
    fun getSearchResultPage(
        @PathParam("searchId") searchId: String,
        @QueryParam("page") page: Int = 0,
        @Context context: SecurityContext,
    ): Response {
        val queryCacheItem = getQueryCacheItem(searchId)
        val aggregateStages = queryCacheItem.aggregateStages.toMutableList().apply {
            add(Aggregates.skip(page * configuration.pageSize))
            add(paginationStage)
        }
        val allAnnotations: MutableList<Map<String, Any>> = mutableListOf()
        for (containerName in accessibleContainers(context.userPrincipal.name)) {
            val annotations: List<Map<String, Any>> =
                mdb.getCollection(containerName)
                    .aggregate(aggregateStages)
                    .map { a -> toAnnotationMap(a, containerName) }
                    .toList()
            allAnnotations.addAll(annotations)
        }
        val annotationPage =
            buildAnnotationPage(uriFactory.globalSearchURL(searchId), allAnnotations, page, 0)
        return Response.ok(annotationPage).build()
    }

    private fun accessibleContainers(name: String): List<String> =
        containerUserDAO.getUserRoles(name).map { it.containerName }.toList()

    @Operation(description = "Get information about the given global search")
    @Timed
    @GET
    @Path("search/{searchId}/info")
    fun getSearchInfo(
        @PathParam("searchId") searchId: String,
        @Context context: SecurityContext,
    ): Response {
        val queryCacheItem = getQueryCacheItem(searchId)
        val query = queryCacheItem.queryMap as Map<String, Any>
        val searchInfo = SearchInfo(
            query = query,
            hits = queryCacheItem.count
        )
        return Response.ok(searchInfo).build()
    }


    private fun getQueryCacheItem(searchId: String): QueryCacheItem = queryCache.getIfPresent(searchId)
        ?: throw NotFoundException("No search results found for this search id. The search might have expired.")

    private fun buildAnnotationPage(
        searchUri: URI, annotations: AnnotationList, page: Int, total: Int,
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
            context = listOf(ANNO_JSONLD_URL),
            id = searchPageUri(searchUri, page),
            partOf = searchUri.toString(),
            startIndex = startIndex,
            items = annotations,
            prev = if (prevPage != null) searchPageUri(searchUri, prevPage) else null,
            next = if (nextPage != null) searchPageUri(searchUri, nextPage) else null
        )
    }

    private fun searchPageUri(searchUri: URI, page: Int) =
        UriBuilder.fromUri(searchUri).queryParam("page", page).build().toString()

    private fun toAnnotationMap(a: Document, containerName: String): Map<String, Any> =
        a.get(ANNOTATION_FIELD, Document::class.java)
            .toMutableMap()
            .apply<MutableMap<String, Any>> {
                put(
                    "id", uriFactory.annotationURL(containerName, a.getString(ANNOTATION_NAME_FIELD))
                )
            }

//    start a task to query the accessible containers
//    use the list of available fields to limit which containers to query
//    sorting the results?
}

