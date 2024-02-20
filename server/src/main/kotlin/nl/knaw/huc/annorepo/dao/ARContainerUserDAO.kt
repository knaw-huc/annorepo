package nl.knaw.huc.annorepo.dao

import com.mongodb.client.MongoClient
import org.bson.Document
import nl.knaw.huc.annorepo.api.ARConst
import nl.knaw.huc.annorepo.api.ContainerUserEntry
import nl.knaw.huc.annorepo.api.Role
import nl.knaw.huc.annorepo.api.UserAccessEntry
import nl.knaw.huc.annorepo.config.AnnoRepoConfiguration

const val FIELD_CONTAINER_NAME = "containerName"
const val FIELD_ROLE = "role"

class ARContainerUserDAO(configuration: AnnoRepoConfiguration, mongoClient: MongoClient) : ContainerUserDAO {
    private val mdb = mongoClient.getDatabase(configuration.databaseName)
    private val containerUserCollection = mdb.getCollection(ARConst.CONTAINER_USER_COLLECTION)

    override fun addContainerUser(containerName: String, userName: String, role: Role) {
        containerUserCollection.insertOne(
            Document(FIELD_CONTAINER_NAME, containerName)
                .append(FIELD_USER_NAME, userName)
                .append(FIELD_ROLE, role)
        )
    }

    override fun getUserRole(containerName: String, userName: String): Role? {
        val doc = containerUserCollection.find(
            Document(FIELD_CONTAINER_NAME, containerName)
                .append(FIELD_USER_NAME, userName)
        ).first()
        return if (doc == null) {
            null
        } else {
            Role.valueOf(doc.getString(FIELD_ROLE))
        }
    }

    override fun getUsersForContainer(containerName: String): List<ContainerUserEntry> =
        containerUserCollection
            .find(Document(FIELD_CONTAINER_NAME, containerName))
            .map(::toContainerUserEntry)
            .toList()

    private fun toContainerUserEntry(d: Document) = ContainerUserEntry(
        userName = d.getString(FIELD_USER_NAME),
        role = Role.valueOf(d.getString(FIELD_ROLE))
    )

    override fun removeContainerUser(containerName: String, userName: String) {
        containerUserCollection.deleteMany(
            Document(FIELD_CONTAINER_NAME, containerName)
                .append(FIELD_USER_NAME, userName)
        )
    }

    override fun getUserRoles(userName: String): List<UserAccessEntry> =
        containerUserCollection
            .find(Document(FIELD_USER_NAME, userName))
            .map(::toUserAccessEntry)
            .toList()

    override fun getAll(): List<UserAccessEntry> =
        containerUserCollection
            .find()
            .map(::toUserAccessEntry)
            .toList()

    private fun toUserAccessEntry(d: Document) = UserAccessEntry(
        containerName = d.getString(FIELD_CONTAINER_NAME),
        userName = d.getString(FIELD_USER_NAME),
        role = Role.valueOf(d.getString(FIELD_ROLE))
    )
}