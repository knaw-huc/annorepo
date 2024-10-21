package nl.knaw.huc.annorepo.service

import com.mongodb.client.MongoClient
import org.apache.logging.log4j.kotlin.logger
import nl.knaw.huc.annorepo.api.ARConst
import nl.knaw.huc.annorepo.api.ARConst.ANNOTATION_NAME_FIELD
import nl.knaw.huc.annorepo.api.IndexType
import nl.knaw.huc.annorepo.api.Role
import nl.knaw.huc.annorepo.config.AnnoRepoConfiguration
import nl.knaw.huc.annorepo.dao.ContainerDAO
import nl.knaw.huc.annorepo.dao.ContainerUserDAO
import nl.knaw.huc.annorepo.dao.UserDAO
import nl.knaw.huc.annorepo.resources.tools.IndexManager
import nl.knaw.huc.annorepo.resources.tools.hasAnnotationNameIndex

class MongoDbUpdater(
    configuration: AnnoRepoConfiguration,
    client: MongoClient,
    private val userDAO: UserDAO,
    val containerUserDAO: ContainerUserDAO,
    private val containerDAO: ContainerDAO,
    val indexManager: IndexManager
) {
    private val mdb = client.getDatabase(configuration.databaseName)

    fun run() {
        updateContainerUsers()
        updateAnnotationNameIndex()
    }

    private fun updateContainerUsers() {
        val allContainerUsers = containerUserDAO.getAll()
        if (allContainerUsers.isEmpty()) { // < v0.5.0
            logger.info { "No container users found, adding all users to all containers as admin" }
            val userNames = userDAO.allUserEntries().map { it.userName }
            for (containerName in allAnnotationContainers()) {
                for (userName in userNames) {
                    containerUserDAO.addContainerUser(containerName, userName, Role.ADMIN)
                }
            }
        } else {
            logger.info { "No container user update necessary" }
        }
    }

    private fun allAnnotationContainers(): List<String> = mdb.listCollectionNames()
        .filter { it != ARConst.CONTAINER_METADATA_COLLECTION }
        .filter { !it.startsWith('_') }
        .sorted<String>()
        .toList()

    private fun updateAnnotationNameIndex() {
        for (containerName in allAnnotationContainers()) {
            logger.info { "Container $containerName" }
            val container = containerDAO.getCollection(containerName)
            if (!container.hasAnnotationNameIndex()) {
                logger.info { "> creating annotation_name index" }
                indexManager.startIndexCreation(
                    containerName = containerName,
                    indexParts = listOf(
                        IndexManager.IndexPart(
                            fieldName = ANNOTATION_NAME_FIELD,
                            indexType = IndexType.HASHED,
                            indexTypeName = "annotation_name)"
                        )
                    )
                )
            }
        }
    }

}

