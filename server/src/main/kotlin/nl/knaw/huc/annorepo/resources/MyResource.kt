package nl.knaw.huc.annorepo.resources

import java.util.*
import jakarta.annotation.security.PermitAll
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.Context
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.core.SecurityContext
import com.codahale.metrics.annotation.Timed
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import org.slf4j.LoggerFactory
import nl.knaw.huc.annorepo.api.ARConst.SECURITY_SCHEME_NAME
import nl.knaw.huc.annorepo.api.ResourcePaths.MY
import nl.knaw.huc.annorepo.auth.ContainerUserDAO
import nl.knaw.huc.annorepo.auth.RootUser

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
    fun getAccessibleContainers(@Context context: SecurityContext): Response =
        if (context.userPrincipal is RootUser) {
            val allContainerNames = containerUserDAO.getAll().map { it.containerName }.toSortedSet()
            val containerNames = mapOf("ROOT" to allContainerNames)
            Response.ok(containerNames).build()
        } else {
            val userName = context.userPrincipal.name
            val userRoles = containerUserDAO.getUserRoles(userName)
            val containerUsersGroupedByRole = userRoles.groupBy { it.role }
            val containerNamesGroupedByRole: TreeMap<String, List<String>> = TreeMap()
            for (role in containerUsersGroupedByRole.keys.sorted()) {
                val containerNames = containerUsersGroupedByRole[role]?.map { it.containerName }
                containerNamesGroupedByRole[role.name] = containerNames!!
            }
            Response.ok(containerNamesGroupedByRole).build()
        }

}

