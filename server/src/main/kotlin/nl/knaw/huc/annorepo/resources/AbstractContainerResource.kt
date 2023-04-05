package nl.knaw.huc.annorepo.resources

import javax.ws.rs.NotFoundException
import javax.ws.rs.core.SecurityContext
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoDatabase
import nl.knaw.huc.annorepo.config.AnnoRepoConfiguration
import nl.knaw.huc.annorepo.resources.tools.ContainerAccessChecker

abstract class AbstractContainerResource(
    private val configuration: AnnoRepoConfiguration,
    client: MongoClient,
    private val containerAccessChecker: ContainerAccessChecker,
) {
    protected val mdb: MongoDatabase = client.getDatabase(configuration.databaseName)

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
        if (!mdb.listCollectionNames().contains(containerName)) {
            throw NotFoundException("Annotation Container '$containerName' not found")
        }
    }

}