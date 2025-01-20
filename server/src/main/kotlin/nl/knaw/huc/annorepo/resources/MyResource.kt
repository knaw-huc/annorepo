package nl.knaw.huc.annorepo.resources

import java.util.TreeMap
import jakarta.annotation.security.PermitAll
import jakarta.ws.rs.GET
import jakarta.ws.rs.NotAuthorizedException
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
import nl.knaw.huc.annorepo.auth.SramUser
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
                val userRoles = when (val user = context.userPrincipal) {
                    is SramUser -> {
                        user.sramGroups
                            .toMutableList()
                            .apply { add(userName) }
                            .flatMap { containerUserDAO.getUserRoles(it) }
                            .toSet()
                    }

                    else -> containerUserDAO.getUserRoles(userName)
                }
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

    @Operation(description = "Show profile data about the authenticated user")
    @Timed
    @GET
    @Path("profile")
    fun getUserProfile(@Context context: SecurityContext): Response {
        val userPrincipal = context.userPrincipal
        return if (userPrincipal is RootUser) {
            val profile = mapOf(
                "user" to "root"
            )
            Response.ok(profile).build()
        } else if (userPrincipal != null) {
            val profile: MutableMap<String, Any> = mutableMapOf(
                "user" to userPrincipal.name
            )
            if (userPrincipal is SramUser) {
                profile["sram_record"] = userPrincipal.record
            }
            Response.ok(profile).build()
        } else {
            throw NotAuthorizedException("No user found for this api key")
        }
    }
}

