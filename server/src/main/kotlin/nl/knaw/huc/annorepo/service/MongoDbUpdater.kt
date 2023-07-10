package nl.knaw.huc.annorepo.service

import com.mongodb.client.MongoClient
import org.slf4j.LoggerFactory
import nl.knaw.huc.annorepo.api.ARConst
import nl.knaw.huc.annorepo.api.Role
import nl.knaw.huc.annorepo.config.AnnoRepoConfiguration
import nl.knaw.huc.annorepo.dao.ContainerUserDAO
import nl.knaw.huc.annorepo.dao.UserDAO

class MongoDbUpdater(
    configuration: AnnoRepoConfiguration,
    client: MongoClient,
    private val userDAO: UserDAO,
    val containerUserDAO: ContainerUserDAO
) {
    val log = LoggerFactory.getLogger(MongoDbUpdater::class.java)
    private val mdb = client.getDatabase(configuration.databaseName)

    fun run() {
        val allContainerUsers = containerUserDAO.getAll()
        if (allContainerUsers.isEmpty()) { // < v0.5.0
            log.info("No container users found, adding all users to all containers as admin")
            val allContainers = mdb.listCollectionNames()
                .filter { it != ARConst.CONTAINER_METADATA_COLLECTION }
                .sorted()
                .toList()
            val userNames = userDAO.allUserEntries().map { it.userName }
            for (containerName in allContainers) {
                for (userName in userNames) {
                    containerUserDAO.addContainerUser(containerName, userName, Role.ADMIN)
                }
            }

        } else {
            log.info("No update necessary")
        }

    }
}