package nl.knaw.huc.annorepo.resources

import com.codahale.metrics.annotation.Timed
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import nl.knaw.huc.annorepo.AnnoRepoConfiguration
import nl.knaw.huc.annorepo.api.ResourcePaths
import nl.knaw.huc.annorepo.db.AnnotationContainerDao
import nl.knaw.huc.annorepo.db.AnnotationDao
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory
import java.util.*
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.UriBuilder

@Api(ResourcePaths.W3C)
@Path(ResourcePaths.W3C)
@Produces(MediaType.APPLICATION_JSON)
class W3CResource(private val configuration: AnnoRepoConfiguration, private val jdbi: Jdbi) {

    private val log = LoggerFactory.getLogger(javaClass)

    @ApiOperation(value = "Show link to the W3C Web Annotation Protocol")
    @Timed
    @GET
    @Produces(MediaType.TEXT_HTML)
    fun getW3CInfo() =
        """<html>This is the endpoint for the <a href="https://www.w3.org/TR/annotation-protocol/">W3C Web Annotation Protocol</a></html>"""

    @ApiOperation(value = "Create an Annotation Container")
    @Timed
    @POST
    fun createContainer(
        @HeaderParam("slug") slug: String?,
    ): Response {
        var name = slug ?: UUID.randomUUID().toString()
        jdbi.open().use { handle ->
            val dao: AnnotationContainerDao = handle.attach(AnnotationContainerDao::class.java)
            if (dao.existsWithName(name)) {
                log.debug("A container with the suggested name $name already exists, generating a new name.")
                name = UUID.randomUUID().toString()
            }
            log.debug("create Container $name")
            val id = dao.add(name)
            val containerData = dao.findById(id)
            val uri = UriBuilder.fromUri(configuration.externalBaseUrl).path(ResourcePaths.W3C).path(name).build()
            return Response.created(uri).entity(containerData).build()
        }
    }

    @ApiOperation(value = "Get an Annotation Container")
    @Timed
    @GET
    @Path("{containerName}")
    fun readContainer(@PathParam("containerName") containerName: String): Response {
        log.debug("read Container $containerName")
        jdbi.open().use { handle ->
            val dao: AnnotationContainerDao = handle.attach(AnnotationContainerDao::class.java)
            val container = dao.findByName(containerName)
            if (container != null) {
                return Response.ok(container).build()
            } else {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity("Container '$containerName' not found")
                    .build()
//                throw NotFoundException("Container '$containerName' not found")
//                throw WebApplicationException("Container '$containerName' not found", Response.Status.NOT_FOUND)
            }
        }
    }

    @ApiOperation(value = "Delete an empty Annotation Container")
    @Timed
    @DELETE
    @Path("{containerName}")
    fun deleteContainer(@PathParam("containerName") containerName: String): Response {
        log.debug("delete Container $containerName")
        jdbi.open().use { handle ->
            val dao: AnnotationContainerDao = handle.attach(AnnotationContainerDao::class.java)
            if (dao.isEmpty(containerName)) {
                dao.deleteByName(containerName)
                return Response.noContent().build()
            } else {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Container $containerName is not empty, all annotations need to be removed from this container first.")
                    .build()
            }
        }
    }

    @ApiOperation(value = "Create an Annotation")
    @Timed
    @POST
    @Path("{containerName}")
    fun createAnnotation(
        @HeaderParam("slug") slug: String?, @PathParam("containerName") containerName: String, annotationJson: String
    ): Response {
        log.info("annotation=\n$annotationJson")
        var name = slug ?: UUID.randomUUID().toString()
        jdbi.open().use { handle ->
            val containerDao: AnnotationContainerDao = handle.attach(AnnotationContainerDao::class.java)
            val containerId = containerDao.findIdByName(containerName)!!
            val annotationDao: AnnotationDao = handle.attach(AnnotationDao::class.java)
            if (annotationDao.existsWithNameInContainer(name, containerId)) {
                log.debug("An annotation with the suggested name $name already exists in container $containerName, generating a new name.")
                name = UUID.randomUUID().toString()
            }
            log.info("create annotation $name in container $containerName")
            val id = annotationDao.add(containerId, name, annotationJson)
            val annotationData = annotationDao.findById(id)
            val uri =
                UriBuilder.fromUri(configuration.externalBaseUrl)
                    .path(ResourcePaths.W3C)
                    .path(containerName)
                    .path(name)
                    .build()
            return Response.created(uri)
                .entity(annotationData)
                .build()
        }
    }

    @ApiOperation(value = "Get an Annotation")
    @Timed
    @GET
    @Path("{containerName}/{annotationName}")
    fun readAnnotation(
        @PathParam("containerName") containerName: String, @PathParam("annotationName") annotationName: String
    ): Response {
        log.debug("read annotation $annotationName in container $containerName")
        jdbi.open().use { handle ->
            val containerDao: AnnotationContainerDao = handle.attach(AnnotationContainerDao::class.java)
            val containerId = containerDao.findIdByName(containerName)!!
            val annotationDao: AnnotationDao = handle.attach(AnnotationDao::class.java)
            val annotationData = annotationDao.findByContainerIdAndName(containerId, annotationName)
            return if (annotationData != null) {
                Response.ok(annotationData).build()
            } else Response.status(Response.Status.NOT_FOUND).build()
        }
    }

    @ApiOperation(value = "Delete an Annotation")
    @Timed
    @DELETE
    @Path("{containerName}/{annotationName}")
    fun deleteAnnotation(
        @PathParam("containerName") containerName: String, @PathParam("annotationName") annotationName: String
    ) {
        log.debug("delete annotation $annotationName in container $containerName")
        jdbi.open().use { handle ->
            val containerDao: AnnotationContainerDao = handle.attach(AnnotationContainerDao::class.java)
            val containerId = containerDao.findIdByName(containerName)!!
            val annotationDao: AnnotationDao = handle.attach(AnnotationDao::class.java)
            annotationDao.deleteByContainerIdAndName(containerId, annotationName)
        }
    }

}