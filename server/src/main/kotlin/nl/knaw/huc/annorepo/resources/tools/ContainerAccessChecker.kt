package nl.knaw.huc.annorepo.resources.tools

import java.security.Principal
import jakarta.ws.rs.NotAuthorizedException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import nl.knaw.huc.annorepo.api.Role
import nl.knaw.huc.annorepo.auth.RootUser
import nl.knaw.huc.annorepo.dao.ContainerUserDAO

class ContainerAccessChecker(private val containerUserDAO: ContainerUserDAO) {

    val log: Logger = LoggerFactory.getLogger(javaClass)

    fun checkUserHasAdminRightsInThisContainer(userPrincipal: Principal?, containerName: String) {
        checkUserRightsInThisContainer(userPrincipal, containerName, setOf(Role.ADMIN), false)
    }

    fun checkUserHasEditRightsInThisContainer(userPrincipal: Principal?, containerName: String) {
        checkUserRightsInThisContainer(userPrincipal, containerName, setOf(Role.ADMIN, Role.EDITOR), false)
    }

    fun checkUserHasReadRightsInThisContainer(
        userPrincipal: Principal?,
        containerName: String,
        anonymousHasAccess: Boolean
    ) = checkUserRightsInThisContainer(
        userPrincipal,
        containerName,
        setOf(Role.ADMIN, Role.EDITOR, Role.GUEST),
        anonymousHasAccess
    )

    private fun checkUserRightsInThisContainer(
        userPrincipal: Principal?,
        containerName: String,
        rolesWithAccessRights: Set<Role>,
        anonymousHasAccess: Boolean = false,
    ) {
//        log.info("userPrincipal={}", userPrincipal)
//        log.info("anonymousHasAccess={}", anonymousHasAccess)
        if (userPrincipal is RootUser) {
            return
        }

        if (userPrincipal != null) {
            val userName = userPrincipal.name
            val role = containerUserDAO.getUserRole(containerName, userName)
            if (role in rolesWithAccessRights) {
                return
            }
            throw NotAuthorizedException("User $userName does not have access rights to this endpoint")
        }

        if (!anonymousHasAccess) {
            throw NotAuthorizedException("No authentication found")
        }
    }

}