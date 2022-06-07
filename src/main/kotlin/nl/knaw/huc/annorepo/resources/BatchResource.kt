package nl.knaw.huc.annorepo.resources

import com.codahale.metrics.annotation.Timed
import com.mongodb.client.MongoClient
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import nl.knaw.huc.annorepo.api.ResourcePaths
import nl.knaw.huc.annorepo.config.AnnoRepoConfiguration
import nl.knaw.huc.annorepo.service.UriFactory
import org.bson.Document
import org.slf4j.LoggerFactory
import java.util.*
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Api(ResourcePaths.MONGOBATCH)
@Path(ResourcePaths.MONGOBATCH)
@Produces(MediaType.APPLICATION_JSON)
class BatchResource(
    configuration: AnnoRepoConfiguration,
    private val client: MongoClient,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val uriFactory = UriFactory(configuration)

    @ApiOperation(value = "Upload annotations in batch to a given container")
    @Timed
    @POST
    @Path("{containerName}/annotations")
    fun postAnnotationsMongo(
        @PathParam("containerName") containerName: String,
        annotations: List<HashMap<String, Any?>>
    ): Response {
        val annotationNames = mutableListOf<String>()
        val mdb = client.getDatabase("annorepo")
        val container = mdb.getCollection(containerName)
        for (i in 0..annotations.size) {
            annotationNames.add(UUID.randomUUID().toString())
        }
        val documents = annotations.mapIndexed { index, annotationMap ->
            val name = annotationNames[index]
            val annotationDocument = Document(annotationMap)
            Document("annotation_name", name).append("annotation", annotationDocument)
        }
        container.insertMany(documents)
        return Response.ok(annotationNames).build()
    }

}