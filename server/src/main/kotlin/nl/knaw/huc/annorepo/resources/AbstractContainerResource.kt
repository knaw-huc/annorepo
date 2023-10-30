package nl.knaw.huc.annorepo.resources

import jakarta.ws.rs.NotFoundException
import jakarta.ws.rs.core.SecurityContext
import nl.knaw.huc.annorepo.config.AnnoRepoConfiguration
import nl.knaw.huc.annorepo.dao.ContainerDAO
import nl.knaw.huc.annorepo.resources.tools.ContainerAccessChecker

abstract class AbstractContainerResource(
    private val configuration: AnnoRepoConfiguration,
    private val containerDAO: ContainerDAO,
    private val containerAccessChecker: ContainerAccessChecker,
) {
//    protected val mdb: MongoDatabase = client.getDatabase(configuration.databaseName)

    protected fun checkUserHasAdminRightsInThisContainer(context: SecurityContext, containerName: String) {
        checkContainerExists(containerName)
        if (configuration.withAuthentication) {
            containerAccessChecker.checkUserHasAdminRightsInThisContainer(context.userPrincipal, containerName)
        }
    }

    protected fun checkUserHasEditRightsInThisContainer(context: SecurityContext, containerName: String) {
        checkContainerExists(containerName)
        if (configuration.withAuthentication) {
            containerAccessChecker.checkUserHasEditRightsInThisContainer(context.userPrincipal, containerName)
        }
    }

    protected fun checkUserHasReadRightsInThisContainer(context: SecurityContext, containerName: String) {
        checkContainerExists(containerName)
        if (configuration.withAuthentication) {
            containerAccessChecker.checkUserHasReadRightsInThisContainer(context.userPrincipal, containerName)
        }
    }

    private fun checkContainerExists(containerName: String) {
        if (!containerDAO.containerExists(containerName)) {
            throw NotFoundException("Annotation Container '$containerName' not found")
        }
    }

}