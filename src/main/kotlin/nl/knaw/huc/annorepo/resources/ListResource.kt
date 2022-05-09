package nl.knaw.huc.annorepo.resources

import com.codahale.metrics.annotation.Timed
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import nl.knaw.huc.annorepo.api.ResourcePaths
import nl.knaw.huc.annorepo.config.AnnoRepoConfiguration
import nl.knaw.huc.annorepo.db.AnnotationContainerDao
import nl.knaw.huc.annorepo.db.AnnotationDao
import nl.knaw.huc.annorepo.service.UriFactory
import org.jdbi.v3.core.Jdbi
import org.json.JSONObject
import org.slf4j.LoggerFactory
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(ResourcePaths.LIST)
@Path(ResourcePaths.LIST)
@Produces(MediaType.APPLICATION_JSON)
class ListResource(private val configuration: AnnoRepoConfiguration, private val jdbi: Jdbi) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val uriFactory = UriFactory(configuration)

    @ApiOperation(value = "Get a list of all the container URLs")
    @Timed
    @GET
    @Path("containers")
    fun getContainerURLs(): List<String> {
        jdbi.open().use { handle ->
            val dao: AnnotationContainerDao = handle.attach(AnnotationContainerDao::class.java)
            return dao.getNames()
                .sorted()
                .map { n -> uriFactory.containerURL(n).toString() }
        }
    }

    @ApiOperation(value = "Get a list of all the annotation URLs")
    @Timed
    @GET
    @Path("annotations")
    fun getAnnotationURLs(): Map<String, Any> {
        jdbi.open().use { handle ->
            val dao: AnnotationDao = handle.attach(AnnotationDao::class.java)
            val urls = dao.allAnnotations()
                .map { a -> uriFactory.annotationURL(a.containerName, a.annotationName).toString() }
            val partOfURL = "${configuration.externalBaseUrl}/${ResourcePaths.LIST}/annotations/"
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