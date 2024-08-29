package nl.knaw.huc.annorepo.resources

import java.util.TreeMap
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
import nl.knaw.huc.annorepo.api.ARConst.SECURITY_SCHEME_NAME
import nl.knaw.huc.annorepo.api.ResourcePaths.MY
import nl.knaw.huc.annorepo.api.Role
import nl.knaw.huc.annorepo.auth.RootUser
import nl.knaw.huc.annorepo.dao.ContainerDAO
import nl.knaw.huc.annorepo.dao.ContainerUserDAO

@Path(MY)
@Produces(MediaType.APPLICATION_JSON)
@PermitAll
@SecurityRequirement(name = SECURITY_SCHEME_NAME)
class MyResource(
    private val containerDAO: ContainerDAO,
    private val containerUserDAO: ContainerUserDAO
) {
    @Operation(description = "List all containers the authenticated user has access to, grouped by role")
    @Timed
    @GET
    @Path("containers")
    fun getAccessibleContainers(@Context context: SecurityContext): Response =
        if (context.userPrincipal is RootUser) {
            val allContainerNames = containerUserDAO.getAll().map { it.containerName }.toSortedSet()
            val containerNames = mapOf(Role.ROOT.name to allContainerNames)
            Response.ok(containerNames).build()
        } else {
            val containersReadOnlyForAnonymous = containerDAO.listCollectionNames()
                .filter { containerDAO.getContainerMetadata(it)?.isReadOnlyForAnonymous ?: false }
                .sorted()
                .toMutableList()
            val accessibleContainerNamesGroupedByRole = if (context.userPrincipal == null) {
                mapOf(Role.GUEST.name to containersReadOnlyForAnonymous)
            } else {
                val userName = context.userPrincipal.name
                val userRoles = containerUserDAO.getUserRoles(userName)
                val containerUsersGroupedByRole = userRoles.groupBy { it.role }
                val containerNamesGroupedByRole: TreeMap<String, MutableList<String>> = TreeMap()
                for (role in containerUsersGroupedByRole.keys.sorted()) {
                    val containerNames = containerUsersGroupedByRole[role]?.map { it.containerName } ?: listOf()
                    containersReadOnlyForAnonymous.removeAll(containerNames.toSet())
                    containerNamesGroupedByRole[role.name] = containerNames.toMutableList()
                }
                if (containersReadOnlyForAnonymous.isNotEmpty()) {
                    if (containerNamesGroupedByRole[Role.GUEST.name] != null) {
                        containerNamesGroupedByRole[Role.GUEST.name]?.addAll(containersReadOnlyForAnonymous)
                    } else {
                        containerNamesGroupedByRole[Role.GUEST.name] = containersReadOnlyForAnonymous
                    }
                }
                containerNamesGroupedByRole
            }
            Response.ok(accessibleContainerNamesGroupedByRole).build()
        }

}

