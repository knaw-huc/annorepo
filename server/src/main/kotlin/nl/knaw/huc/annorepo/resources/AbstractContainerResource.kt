package nl.knaw.huc.annorepo.resources

import jakarta.ws.rs.NotAuthorizedException
import jakarta.ws.rs.NotFoundException
import jakarta.ws.rs.core.SecurityContext
import nl.knaw.huc.annorepo.api.ContainerMetadata
import nl.knaw.huc.annorepo.config.AnnoRepoConfiguration
import nl.knaw.huc.annorepo.dao.ContainerDAO
import nl.knaw.huc.annorepo.resources.tools.ContainerAccessChecker

abstract class AbstractContainerResource(
    private val configuration: AnnoRepoConfiguration,
    private val containerDAO: ContainerDAO,
    private val containerAccessChecker: ContainerAccessChecker,
) {

    protected fun SecurityContext.checkUserHasAdminRightsInThisContainer(containerName: String) {
        checkContainerExists(containerName)
        if (configuration.withAuthentication) {
            containerAccessChecker.checkUserHasAdminRightsInThisContainer(userPrincipal, containerName)
        }
    }

    protected fun SecurityContext.checkUserHasEditRightsInThisContainer(containerName: String) {
        checkContainerExists(containerName)
        if (configuration.withAuthentication) {
            containerAccessChecker.checkUserHasEditRightsInThisContainer(userPrincipal, containerName)
        }
    }

    protected fun SecurityContext.checkUserHasReadRightsInThisContainer(containerName: String) {
        checkContainerExists(containerName)
        val anonymousHasAccess = checkContainerIsReadOnlyForAnonymous(containerName)
        if (configuration.withAuthentication) {
            containerAccessChecker.checkUserHasReadRightsInThisContainer(
                userPrincipal,
                containerName,
                anonymousHasAccess
            )
        }
    }

    protected fun SecurityContext.checkUserHasContainerCreationRights() {
        if (configuration.withAuthentication && userPrincipal == null) {
            throw NotAuthorizedException("Anonymous user does not have access rights to this endpoint")
        }
    }

    private fun checkContainerIsReadOnlyForAnonymous(containerName: String): Boolean {
        val containerMetadata: ContainerMetadata? =
            containerDAO.getContainerMetadata(containerName)
        return containerMetadata?.isReadOnlyForAnonymous ?: false
    }

    private fun checkContainerExists(containerName: String) {
        if (!containerDAO.containerExists(containerName)) {
            throw NotFoundException("Annotation Container '$containerName' not found")
        }
    }

}