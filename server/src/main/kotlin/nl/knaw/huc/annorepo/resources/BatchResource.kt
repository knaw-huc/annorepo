package nl.knaw.huc.annorepo.resources

import com.codahale.metrics.annotation.Timed
import com.mongodb.client.MongoClient
import io.swagger.v3.oas.annotations.Operation
import nl.knaw.huc.annorepo.api.ResourcePaths
import nl.knaw.huc.annorepo.config.AnnoRepoConfiguration
import org.bson.Document
import java.util.*
import javax.annotation.security.PermitAll
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.Context
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.SecurityContext

@Path(ResourcePaths.BATCH)
@Produces(MediaType.APPLICATION_JSON)
@PermitAll
class BatchResource(
    private val configuration: AnnoRepoConfiguration,
    private val client: MongoClient,
) {
//    private val log = LoggerFactory.getLogger(javaClass)
//    private val uriFactory = UriFactory(configuration)

    @Operation(description = "Upload annotations in batch to a given container")
    @Timed
    @POST
    @Path("{containerName}/annotations")
    fun postAnnotationsBatch(
        @PathParam("containerName") containerName: String,
        annotations: List<HashMap<String, Any>>,
        @Context context: SecurityContext
    ): Response {
        val annotationNames = mutableListOf<String>()
        val mdb = client.getDatabase(configuration.databaseName)
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