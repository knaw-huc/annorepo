package nl.knaw.huc.annorepo.resources

import com.codahale.metrics.annotation.Timed
import com.mongodb.client.MongoClient
import com.mongodb.client.model.Filters.exists
import io.swagger.v3.oas.annotations.Operation
import nl.knaw.huc.annorepo.api.ARConst.ANNOTATION_MEDIA_TYPE
import nl.knaw.huc.annorepo.api.ResourcePaths
import nl.knaw.huc.annorepo.config.AnnoRepoConfiguration
import nl.knaw.huc.annorepo.service.UriFactory
import org.bson.Document
import org.litote.kmongo.aggregate
import org.litote.kmongo.match
import org.slf4j.LoggerFactory
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam

@Path(ResourcePaths.LIST)
@Produces(ANNOTATION_MEDIA_TYPE)
class ListResource(
    private val configuration: AnnoRepoConfiguration, client: MongoClient
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val uriFactory = UriFactory(configuration)
    private val mdb = client.getDatabase(configuration.databaseName)

    @Operation(description = "Get a list of all the container URLs")
    @Timed
    @GET
    @Path("containers")
    fun getContainerURLs(): List<String> =
        mdb.listCollectionNames().map {
            uriFactory.containerURL(it).toString()
        }.toList()

    private val field = "annotation_name"

    @Operation(description = "Get a list of all the annotation URLs")
    @Timed
    @GET
    @Path("{containerName}/annotations")
    fun getAnnotationURLs(
        @PathParam("containerName") containerName: String,
        @QueryParam("start") offset: Int = 0
    ): List<String> =
        mdb.getCollection(containerName).aggregate<Document>(
            match(exists(field)),
        )
            .map { d -> uriFactory.annotationURL(containerName, d.getString(field)).toString() }
            .toList()
            .sorted()
            .subList(fromIndex = offset, toIndex = offset + configuration.pageSize)

//    private fun annotationPage(
//        urls: List<String>,
//        partOfURL: String,
//        startIndex: Int
//    ) = JSONObject(
//        mapOf(
//            "@context" to listOf(
//                "http://www.w3.org/ns/anno.jsonld",
//                "http://www.w3.org/ns/ldp.jsonld"
//            ),
//            "type" to "AnnotationPage",
//            "as:items" to mapOf("@list" to urls),
//            "partOf" to partOfURL,
//            "startIndex" to startIndex
//        )
//    ).toMap()

}