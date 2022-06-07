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
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import nl.knaw.huc.annorepo.api.ResourcePaths
import nl.knaw.huc.annorepo.config.AnnoRepoConfiguration
import nl.knaw.huc.annorepo.service.UriFactory
import org.bson.Document
import org.eclipse.jetty.util.ajax.JSON
import org.json.JSONObject
import org.litote.kmongo.aggregate
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Api(ResourcePaths.SEARCH)
@Path(ResourcePaths.SEARCH)
@Produces(MediaType.APPLICATION_JSON)
class SearchResource(
    private val configuration: AnnoRepoConfiguration,
    client: MongoClient
) {
    //    private val log = LoggerFactory.getLogger(javaClass)
    private val uriFactory = UriFactory(configuration)
    private val mdb = client.getDatabase("annorepo")

    private val annotationProjectStage = project(Document("annotation", 1).append("_id", 0))
    private val paginationStage = limit(configuration.pageSize)

    @ApiOperation(value = "Find annotations in the given container with all the given field values")
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
            val resultPage =
                container.aggregate(aggregateStages).map { a -> toAnnotationMap(a, containerName) }.toList()
            val partOfURL =
                "${configuration.externalBaseUrl}/${ResourcePaths.SEARCH}/$containerName/annotations"
            val startIndex = 0
            val entity = annotationPage(resultPage, partOfURL, startIndex)
            return Response.ok(entity).build()
        }
        return Response.status(Response.Status.BAD_REQUEST).build()
    }

    private val withinRange = "within_range"
    private val selectorType = "urn:example:republic:TextAnchorSelector"

    @ApiOperation(value = "Find annotations within the given range")
    @Timed
    @GET
    @Path("{containerName}/within_range")
    fun findAnnotationsInContainerWithinRange(
        @PathParam("containerName") containerName: String,
        @QueryParam("target.source") targetSource: String,
        @QueryParam("range.start") rangeStart: Float,
        @QueryParam("range.end") rangeEnd: Float,
        @QueryParam("startIndex") startIndex: Int = 0
    ): Response {
        val collection = mdb.getCollection(containerName)
        val annotationFieldPrefix = "annotation."
        val annotations = collection.aggregate<Document>(
            match(
                and(
                    eq("${annotationFieldPrefix}target.source", targetSource),
                    eq("${annotationFieldPrefix}target.selector.type", selectorType),
                    gte("${annotationFieldPrefix}target.selector.start", rangeStart),
                    lte("${annotationFieldPrefix}target.selector.end", rangeEnd),
                )
            ),
            skip(startIndex), // start at offset
            paginationStage // return $pageSize documents or less
        )
            .map { document -> toAnnotationMap(document, containerName) }
            .toList()
        val partOfURL =
            "${configuration.externalBaseUrl}/${ResourcePaths.SEARCH}/$containerName/$withinRange?target.source=$targetSource&range.start=$rangeStart&range.end=$rangeEnd"
        val entity = annotationPage(annotations, partOfURL, startIndex)
        return Response.ok(entity).build()
    }

    @ApiOperation(value = "Find annotations that overlap with the given range")
    @Timed
    @GET
    @Path("{containerName}/overlapping_with_range")
    fun findAnnotationsInContainerOverlappingWithRange(
        @PathParam("containerName") containerName: String,
        @QueryParam("target.source") targetSource: String,
        @QueryParam("range.start") rangeStart: Float,
        @QueryParam("range.end") rangeEnd: Float,
        @QueryParam("startIndex") startIndex: Int = 0
    ): Response {
        val collection = mdb.getCollection(containerName)
        val annotationFieldPrefix = "annotation."
        val annotations = collection.aggregate<Document>(
            match(
                and(
                    eq("${annotationFieldPrefix}target.source", targetSource),
                    eq("${annotationFieldPrefix}target.selector.type", selectorType),
                    lt("${annotationFieldPrefix}target.selector.start", rangeEnd),
                    gt("${annotationFieldPrefix}target.selector.end", rangeStart),
                )
            ),
            skip(startIndex), // start at offset
            paginationStage // return $pageSize documents or less
        )
            .map { document -> toAnnotationMap(document, containerName) }
            .toList()
        val partOfURL =
            "${configuration.externalBaseUrl}/${ResourcePaths.SEARCH}/$containerName/overlapping_with_range?target.source=$targetSource&range.start=$rangeStart&range.end=$rangeEnd"
        val entity = annotationPage(annotations, partOfURL, startIndex)
        return Response.ok(entity).build()
    }

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
                "http://www.w3.org/ns/anno.jsonld",
                "http://www.w3.org/ns/ldp.jsonld"
            ),
            "type" to "AnnotationPage",
            "as:items" to mapOf("@list" to urls),
            "partOf" to partOfURL,
            "startIndex" to startIndex
        )
    ).toMap()

}