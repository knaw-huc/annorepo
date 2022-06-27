package nl.knaw.huc.annorepo.resources

import com.codahale.metrics.annotation.Timed
import com.mongodb.client.MongoClient
import com.mongodb.client.model.Aggregates.limit
import com.mongodb.client.model.Aggregates.match
import com.mongodb.client.model.Aggregates.project
import com.mongodb.client.model.Aggregates.skip
import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Filters.gt
import com.mongodb.client.model.Filters.gte
import com.mongodb.client.model.Filters.lt
import com.mongodb.client.model.Filters.lte
import io.swagger.v3.oas.annotations.Operation
import nl.knaw.huc.annorepo.api.ARConst.ANNO_JSONLD_URL
import nl.knaw.huc.annorepo.api.ARConst.LDP_JSONLD_URL
import nl.knaw.huc.annorepo.api.AnnotationPage
import nl.knaw.huc.annorepo.api.ResourcePaths.SEARCH
import nl.knaw.huc.annorepo.config.AnnoRepoConfiguration
import nl.knaw.huc.annorepo.service.UriFactory
import org.bson.Document
import org.bson.conversions.Bson
import org.eclipse.jetty.util.ajax.JSON
import org.json.JSONObject
import org.litote.kmongo.aggregate
import java.net.URI
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.UriBuilder

@Path(SEARCH)
@Produces(MediaType.APPLICATION_JSON)
class SearchResource(
    private val configuration: AnnoRepoConfiguration,
    client: MongoClient
) {
    //    private val log = LoggerFactory.getLogger(javaClass)
    private val uriFactory = UriFactory(configuration)
    private val mdb = client.getDatabase(configuration.databaseName)

    private val annotationProjectStage = project(Document("annotation", 1).append("_id", 0))
    private val paginationStage = limit(configuration.pageSize)

    private val withinRange = "within_range"
    private val overlappingWithRange = "overlapping_with_range"
    private val selectorType = "urn:example:republic:TextAnchorSelector"
    private val annotationFieldPrefix = "annotation."

    @Operation(description = "Find annotations in the given container with all the given field values")
    @Timed
    @POST
    @Path("{containerName}/annotations")
    fun findAnnotationsInContainer(
        @PathParam("containerName") containerName: String,
        queryJson: String
    ): Response {
        val container = mdb.getCollection(containerName)
        var queryMap = JSON.parse(queryJson)
        if (queryMap is HashMap<*, *>) {
            queryMap = queryMap.toMutableMap()
            val aggregateStages = queryMap.map { (k, v) ->
                match(eq("annotation.$k", v))
            }.toMutableList().apply {
                add(skip(0))
                add(paginationStage)
                add(annotationProjectStage)
            }
            val annotations =
                container.aggregate(aggregateStages).map { a -> toAnnotationMap(a, containerName) }.toList()
            val searchURL =
                "${configuration.externalBaseUrl}/$SEARCH/$containerName/annotations"
            val startIndex = 0
            val entity = annotationPage(annotations, searchURL, startIndex)
            return Response.ok(entity).build()
        }
        return Response.status(Response.Status.BAD_REQUEST).build()
    }

    @Operation(description = "Find annotations within the given range")
    @Timed
    @GET
    @Path("{containerName}/within_range")
    fun findAnnotationsInContainerWithinRange(
        @PathParam("containerName") containerName: String,
        @QueryParam("target.source") targetSource: String,
        @QueryParam("range.start") rangeStart: Float,
        @QueryParam("range.end") rangeEnd: Float,
        @QueryParam("page") page: Int = 0
    ): Response =
        filterContainerAnnotationsWithRange(
            searchType = withinRange,
            matchPattern = and(
                eq("${annotationFieldPrefix}target.source", targetSource),
                eq("${annotationFieldPrefix}target.selector.type", selectorType),
                gte("${annotationFieldPrefix}target.selector.start", rangeStart),
                lte("${annotationFieldPrefix}target.selector.end", rangeEnd),
            ),
            containerName = containerName,
            targetSource = targetSource,
            rangeStart = rangeStart,
            rangeEnd = rangeEnd,
            page = page
        )

    @Operation(description = "Find annotations that overlap with the given range")
    @Timed
    @GET
    @Path("{containerName}/overlapping_with_range")
    fun findAnnotationsInContainerOverlappingWithRange(
        @PathParam("containerName") containerName: String,
        @QueryParam("target.source") targetSource: String,
        @QueryParam("range.start") rangeStart: Float,
        @QueryParam("range.end") rangeEnd: Float,
        @QueryParam("page") page: Int = 0
    ): Response =
        filterContainerAnnotationsWithRange(
            searchType = overlappingWithRange,
            matchPattern = and(
                eq("${annotationFieldPrefix}target.source", targetSource),
                eq("${annotationFieldPrefix}target.selector.type", selectorType),
                lt("${annotationFieldPrefix}target.selector.start", rangeEnd),
                gt("${annotationFieldPrefix}target.selector.end", rangeStart),
            ),
            containerName = containerName,
            targetSource = targetSource,
            rangeStart = rangeStart,
            rangeEnd = rangeEnd,
            page = page
        )

    private fun filterContainerAnnotationsWithRange(
        searchType: String,
        matchPattern: Bson,
        containerName: String,
        targetSource: String,
        rangeStart: Float,
        rangeEnd: Float,
        page: Int
    ): Response {
        val collection = mdb.getCollection(containerName)
        val offset = page * configuration.pageSize
        val annotations = collection.aggregate<Document>(
            match(matchPattern),
            skip(offset), // start at offset
            paginationStage // return $pageSize documents or less
        )
            .map { document -> toAnnotationMap(document, containerName) }
            .toList()
        val uri = UriBuilder.fromUri(configuration.externalBaseUrl)
            .path(SEARCH)
            .path(containerName)
            .path(searchType)
            .queryParam("target.source", targetSource)
            .queryParam("range.start", rangeStart)
            .queryParam("range.end", rangeEnd)
            .build()
        val annotationPage = buildAnnotationPage(uri, annotations, page)
        return Response.ok(annotationPage).build()
    }

    private fun buildAnnotationPage(
        searchUri: URI,
        annotations: List<Map<String, Any>>,
        page: Int
    ): AnnotationPage {
        val prevPage = if (page > 0) {
            page - 1
        } else {
            null
        }
        val nextPage = if (annotations.size == configuration.pageSize) {
            page + 1
        } else {
            null
        }

        return AnnotationPage(
            id = searchPageUri(searchUri, page),
            partOf = searchUri.toString(),
            startIndex = page,
            items = annotations,
            prev = if (prevPage != null) searchPageUri(searchUri, prevPage) else null,
            next = if (nextPage != null) searchPageUri(searchUri, nextPage) else null
        )
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

    private fun annotationPage(
        urls: List<Map<String, Any>>,
        partOfURL: String,
        startIndex: Int
    ) = JSONObject(
        mapOf(
            "@context" to listOf(
                ANNO_JSONLD_URL,
                LDP_JSONLD_URL
            ),
            "type" to "AnnotationPage",
            "as:items" to mapOf("@list" to urls),
            "partOf" to partOfURL,
            "startIndex" to startIndex
        )
    ).toMap()

}