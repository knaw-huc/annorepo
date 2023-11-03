package nl.knaw.huc.annorepo.resources

import jakarta.ws.rs.NotFoundException
import jakarta.ws.rs.core.SecurityContext
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoDatabase
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import nl.knaw.huc.annorepo.api.ContainerMetadata
import nl.knaw.huc.annorepo.config.AnnoRepoConfiguration
import nl.knaw.huc.annorepo.dao.ContainerDAO
import nl.knaw.huc.annorepo.resources.tools.ContainerAccessChecker

abstract class AbstractContainerResource(
    private val configuration: AnnoRepoConfiguration,
    client: MongoClient,
    private val containerDAO: ContainerDAO,
    private val containerAccessChecker: ContainerAccessChecker,
) {
    protected val mdb: MongoDatabase = client.getDatabase(configuration.databaseName)
    val log: Logger = LoggerFactory.getLogger(javaClass)

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

    protected fun checkUserHasReadRightsInThisContainer(
        context: SecurityContext,
        containerName: String
    ) {
        checkContainerExists(containerName)
        val anonymousHasAccess = checkContainerIsReadOnlyForAnonymous(containerName)
        if (configuration.withAuthentication) {
            containerAccessChecker.checkUserHasReadRightsInThisContainer(
                context.userPrincipal,
                containerName,
                anonymousHasAccess
            )
        }
    }

    private fun checkContainerIsReadOnlyForAnonymous(containerName: String): Boolean {
        val containerMetadata: ContainerMetadata =
            containerDAO.getContainerMetadata(containerName)!!
        return containerMetadata.isReadOnlyForAnonymous
    }

    private fun checkContainerExists(containerName: String) {
        if (!mdb.listCollectionNames().contains(containerName)) {
            throw NotFoundException("Annotation Container '$containerName' not found")
        }
    }

}