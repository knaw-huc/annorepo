package nl.knaw.huc.annorepo.resources

import com.codahale.metrics.annotation.Timed
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import nl.knaw.huc.annorepo.api.ARConst.SECURITY_SCHEME_NAME
import nl.knaw.huc.annorepo.api.ResourcePaths.MY
import nl.knaw.huc.annorepo.auth.ContainerUserDAO
import org.slf4j.LoggerFactory
import javax.annotation.security.PermitAll
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.Context
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.SecurityContext

@Path(MY)
@Produces(MediaType.APPLICATION_JSON)
@PermitAll
@SecurityRequirement(name = SECURITY_SCHEME_NAME)
class MyResource(
    private val containerUserDAO: ContainerUserDAO
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Operation(description = "List all containers the authenticated user has access to, grouped by role")
    @Timed
    @GET
    @Path("containers")
    fun getAccessibleContainers(@Context context: SecurityContext): Response {
        val accessibleContainers = AccessibleContainerList()
        val userName = context.userPrincipal.name
//        val userRoles = containerUserDAO.getUserRoles()
        return Response.ok(accessibleContainers).build()
    }

}

