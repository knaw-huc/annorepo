package nl.knaw.huc.annorepo.resources

import co.elastic.clients.elasticsearch.ElasticsearchClient
import co.elastic.clients.elasticsearch.core.SearchRequest
import co.elastic.clients.json.JsonData
import com.codahale.metrics.annotation.Timed
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import nl.knaw.huc.annorepo.api.ResourcePaths
import nl.knaw.huc.annorepo.config.AnnoRepoConfiguration
import nl.knaw.huc.annorepo.db.AnnotationDao
import nl.knaw.huc.annorepo.service.UriFactory
import org.jdbi.v3.core.Jdbi
import org.json.JSONObject
import org.slf4j.LoggerFactory
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(ResourcePaths.SEARCH)
@Path(ResourcePaths.SEARCH)
@Produces(MediaType.APPLICATION_JSON)
class SearchResource(
    private val configuration: AnnoRepoConfiguration,
    private val jdbi: Jdbi,
    private val esClient: ElasticsearchClient
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val uriFactory = UriFactory(configuration)

    @ApiOperation(value = "Search for annotations")
    @Timed
    @GET
    @Path("annotations")
    fun getAnnotationSearchResults(@QueryParam("container") containerName: String): Map<String, Any> {
        val searchRequest = SearchRequest.Builder()
            .apply { index(containerName) }
            .size(100)
            .build()
        val results = esClient.search(searchRequest, JsonData::class.java)
        val hitIds = results.hits().hits().map { h -> h.id().toLong() }
        jdbi.open().use { handle ->
            val dao: AnnotationDao = handle.attach(AnnotationDao::class.java)
            val hitIds1 = hitIds.joinToString(separator = ",", prefix = "(", postfix = ")")
            log.info("hitIds1=$hitIds1")
            val urls = dao.annotationsById(hitIds)
                .map { a -> uriFactory.annotationURL(a.containerName, a.annotationName).toString() }
            val partOfURL =
                "${configuration.externalBaseUrl}/${ResourcePaths.SEARCH}/annotations?container=$containerName"
            val startIndex = 0
            return annotationPage(urls, partOfURL, startIndex)
        }
    }

    private fun annotationPage(
        urls: List<String>,
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