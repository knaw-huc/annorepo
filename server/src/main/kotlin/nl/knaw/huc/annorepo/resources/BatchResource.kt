package nl.knaw.huc.annorepo.resources

import jakarta.annotation.security.PermitAll
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.Context
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.core.SecurityContext
import com.codahale.metrics.annotation.Timed
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import nl.knaw.huc.annorepo.api.ARConst.SECURITY_SCHEME_NAME
import nl.knaw.huc.annorepo.api.ResourcePaths
import nl.knaw.huc.annorepo.api.WebAnnotationAsMap
import nl.knaw.huc.annorepo.config.AnnoRepoConfiguration
import nl.knaw.huc.annorepo.dao.ContainerDAO
import nl.knaw.huc.annorepo.resources.tools.ContainerAccessChecker

@Path(ResourcePaths.BATCH)
@Produces(MediaType.APPLICATION_JSON)
@PermitAll
@SecurityRequirement(name = SECURITY_SCHEME_NAME)
class BatchResource(
    configuration: AnnoRepoConfiguration,
    private val containerDAO: ContainerDAO,
    containerAccessChecker: ContainerAccessChecker,
) : AbstractContainerResource(configuration, containerDAO, containerAccessChecker) {
//    private val log = LoggerFactory.getLogger(javaClass)
//    private val uriFactory = UriFactory(configuration)

    @Operation(description = "Upload annotations in batch to a given container")
    @Timed
    @POST
    @Path("{containerName}/annotations")
    @Deprecated("use postAnnotationsBatch in ContainerServiceResource")
    fun postAnnotationsBatch(
        @PathParam("containerName") containerName: String,
        annotations: List<WebAnnotationAsMap>,
        @Context context: SecurityContext,
    ): Response {
        checkUserHasEditRightsInThisContainer(context, containerName)
        val annotationIdentifiers = containerDAO.addAnnotationsInBatch(containerName, annotations)
        return Response.ok(annotationIdentifiers).build()
    }

}