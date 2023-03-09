package nl.knaw.huc.annorepo.auth

import com.mongodb.client.MongoClient
import nl.knaw.huc.annorepo.api.ARConst
import nl.knaw.huc.annorepo.api.Role
import nl.knaw.huc.annorepo.config.AnnoRepoConfiguration

class ARContainerUserDAO(configuration: AnnoRepoConfiguration, mongoClient: MongoClient) : ContainerUserDAO {
    private val mdb = mongoClient.getDatabase(configuration.databaseName)
    private val userCollection = mdb.getCollection(ARConst.CONTAINER_USER_COLLECTION)
    override fun addContainerUser(containerName: String, userName: String, role: Role) {
        TODO("Not yet implemented")
    }

    override fun getUserRole(containerName: String, userName: String): Role? {
        TODO("Not yet implemented")
    }

    override fun getUsersForContainer(containerName: String): List<String> {
        TODO("Not yet implemented")
    }

    override fun removeContainerUser(containerName: String, userName: String) {
        TODO("Not yet implemented")
    }
}