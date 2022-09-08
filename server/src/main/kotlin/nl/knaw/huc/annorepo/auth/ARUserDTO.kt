package nl.knaw.huc.annorepo.auth

import com.mongodb.client.MongoClient
import com.mongodb.client.model.Indexes
import nl.knaw.huc.annorepo.api.ARConst.USER_COLLECTION
import nl.knaw.huc.annorepo.config.AnnoRepoConfiguration
import org.bson.Document

private const val FIELD_API_KEY = "apiKey"
private const val FIELD_USER_NAME = "userName"

class ARUserDTO(
    private val configuration: AnnoRepoConfiguration,
    mongoClient: MongoClient
) : UserDTO {

    private val mdb = mongoClient.getDatabase(configuration.databaseName)
    private val collection = mdb.getCollection(USER_COLLECTION)

    init {
        val apiKeyIndex = Indexes.hashed(FIELD_API_KEY)
        if (!collection.listIndexes().toSet().contains(apiKeyIndex)) {
            collection.createIndex(apiKeyIndex)
        }
    }

    override fun userForApiKey(apiKey: String?): User? =
        when (apiKey) {
            null -> {
                null
            }

            configuration.rootApiKey -> {
                User(":root:")
            }

            else -> {
                val doc = collection.find(Document(FIELD_API_KEY, apiKey)).first()
                if (doc == null) {
                    null
                } else {
                    User(doc.getString(FIELD_USER_NAME))
                }
            }
        }

    fun addEntry(apiKey: String, userName: String) {
        collection.insertOne(Document(FIELD_API_KEY, apiKey).append(FIELD_USER_NAME, userName))
    }

}