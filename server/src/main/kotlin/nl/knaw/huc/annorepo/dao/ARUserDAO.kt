package nl.knaw.huc.annorepo.dao

import com.mongodb.client.MongoClient
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Indexes
import com.mongodb.client.model.Projections
import org.bson.Document
import org.litote.kmongo.excludeId
import org.litote.kmongo.findOne
import nl.knaw.huc.annorepo.api.ARConst.USER_COLLECTION
import nl.knaw.huc.annorepo.api.RejectedUserEntry
import nl.knaw.huc.annorepo.api.UserAddResults
import nl.knaw.huc.annorepo.api.UserEntry
import nl.knaw.huc.annorepo.auth.BasicUser
import nl.knaw.huc.annorepo.auth.RootUser
import nl.knaw.huc.annorepo.auth.User
import nl.knaw.huc.annorepo.config.AnnoRepoConfiguration

const val FIELD_API_KEY = "apiKey"
const val FIELD_USER_NAME = "userName"

class ARUserDAO(
    private val configuration: AnnoRepoConfiguration, mongoClient: MongoClient,
) : UserDAO {

    private val mdb = mongoClient.getDatabase(configuration.databaseName)
    private val userCollection = mdb.getCollection(USER_COLLECTION)
    private val rootApiKey = configuration.rootApiKey

    init {
        val apiKeyIndex = Indexes.hashed(FIELD_API_KEY)
        if (!userCollection.listIndexes().toSet().contains(apiKeyIndex)) {
            userCollection.createIndex(apiKeyIndex)
        }
    }

    override fun userForApiKey(apiKey: String?): User? =
        when (apiKey) {
            null -> null
            configuration.rootApiKey -> RootUser()
            else -> {
                val doc = userCollection.find(Document(FIELD_API_KEY, apiKey)).first()
                if (doc == null) {
                    null
                } else {
                    BasicUser(doc.getString(FIELD_USER_NAME))
                }
            }
        }

    override fun addUserEntries(userEntries: List<UserEntry>): UserAddResults {
        val added = mutableListOf<String>()
        val rejected = mutableListOf<RejectedUserEntry>()
        for (userEntry in userEntries) {
            when {
                userEntry.apiKey == rootApiKey || apiKeyExistsInCollection(userEntry) ->
                    rejected.add(
                        RejectedUserEntry(
                            userEntry = asMap(userEntry),
                            reason = "apiKey already in use"
                        )
                    )

                userNameExistsInCollection(userEntry) ->
                    rejected.add(
                        RejectedUserEntry(
                            userEntry = asMap(userEntry),
                            reason = "userName already in use"
                        )
                    )

                else -> {
                    userCollection.insertOne(
                        Document(FIELD_USER_NAME, userEntry.userName)
                            .append(FIELD_API_KEY, userEntry.apiKey)
                    )
                    added.add(userEntry.userName)
                }
            }
        }
        return UserAddResults(added, rejected)
    }

    override fun allUserEntries(): List<UserEntry> {
        return userCollection.find()
            .projection(Projections.fields(Projections.include(FIELD_USER_NAME, FIELD_API_KEY), excludeId()))
            .sort(Indexes.ascending(FIELD_USER_NAME))
            .map { d -> UserEntry(userName = d.getString(FIELD_USER_NAME), apiKey = d.getString(FIELD_API_KEY)) }
            .toList()
    }

    override fun deleteUsersByName(userNames: Collection<String>): Boolean =
        userCollection
            .deleteMany(Filters.`in`(FIELD_USER_NAME, userNames))
            .deletedCount == userNames.size.toLong()

    private fun userNameExistsInCollection(ue: UserEntry) =
        userCollection.findOne(Document(FIELD_USER_NAME, ue.userName)) != null

    private fun apiKeyExistsInCollection(ue: UserEntry) =
        userCollection.findOne(Document(FIELD_API_KEY, ue.apiKey)) != null

    companion object {
        private fun asMap(userEntry: UserEntry) =
            mapOf(FIELD_USER_NAME to userEntry.userName, FIELD_API_KEY to userEntry.apiKey)
    }

}
