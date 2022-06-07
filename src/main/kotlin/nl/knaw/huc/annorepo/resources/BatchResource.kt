package nl.knaw.huc.annorepo.resources

import com.codahale.metrics.annotation.Timed
import com.fasterxml.jackson.databind.ObjectMapper
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import nl.knaw.huc.annorepo.api.ElasticsearchWrapper
import nl.knaw.huc.annorepo.api.ResourcePaths
import nl.knaw.huc.annorepo.config.AnnoRepoConfiguration
import nl.knaw.huc.annorepo.db.AnnotationContainerDao
import nl.knaw.huc.annorepo.db.AnnotationDao
import nl.knaw.huc.annorepo.service.UriFactory
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory
import java.util.*
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Api(ResourcePaths.BATCH)
@Path(ResourcePaths.BATCH)
@Produces(MediaType.APPLICATION_JSON)
class BatchResource(
    configuration: AnnoRepoConfiguration,
    private val jdbi: Jdbi,
    private val es: ElasticsearchWrapper
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val uriFactory = UriFactory(configuration)

    @ApiOperation(value = "Upload annotations in batch to a given container")
    @Timed
    @POST
    @Path("{containerName}/annotations")
    fun postAnnotations(
        @PathParam("containerName") containerName: String,
        annotations: List<HashMap<String, Any?>>
    ): Response {
        val annotationNames = mutableListOf<String>()
        val indexResult = jdbi.open().use { handle ->
            val containerDao: AnnotationContainerDao = handle.attach(AnnotationContainerDao::class.java)
            val containerId = containerDao.findIdByName(containerName)!!
            val annotationDao: AnnotationDao = handle.attach(AnnotationDao::class.java)
            val bulkOperations = annotations.map { annotation ->
                val name = UUID.randomUUID().toString()
                val annotationJson = ObjectMapper().writeValueAsString(annotation)
                val id = annotationDao.add(containerId, name, annotationJson)
                annotationNames.add(name)

                val json = normalizedAnnotationJson(annotation)
                ESIndexBulkOperation(containerName, id.toString(), name, json)
            }
            es.bulkIndex(bulkOperations)
        }
        return if (indexResult.success) {
            Response.ok(annotationNames).build()
        } else {
            Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(mapOf("index_errors" to indexResult.errors))
                .build()
        }
    }

    private fun normalizedAnnotationJson(annotation: HashMap<String, Any?>): String {
        val normalizedAnnotation = annotation.apply { remove("@context") }
        return ObjectMapper().writeValueAsString(normalizedAnnotation)
    }

}