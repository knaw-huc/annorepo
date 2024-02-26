package nl.knaw.huc.annorepo.dao

import com.mongodb.client.MongoClient
import com.mongodb.client.model.Filters
import org.litote.kmongo.findOne
import org.litote.kmongo.getCollection
import nl.knaw.huc.annorepo.api.ARConst
import nl.knaw.huc.annorepo.config.AnnoRepoConfiguration

class ARCustomQueryDAO(
    configuration: AnnoRepoConfiguration,
    mongoClient: MongoClient
) : CustomQueryDAO {
    private val mdb = mongoClient.getDatabase(configuration.databaseName)
    private val customQueryCollection = mdb.getCollection<CustomQuery>(ARConst.CUSTOM_QUERY_COLLECTION)

    override fun getAllCustomQueries(): List<CustomQuery> =
        customQueryCollection.find().sortedBy { cq -> cq.name.lowercase() }

    override fun nameIsTaken(name: String): Boolean = getByName(name) != null

    override fun getByName(name: String): CustomQuery? {
        return customQueryCollection.findOne(Filters.eq("name", name))
    }

    override fun store(query: CustomQuery) {
        if (nameIsTaken(query.name)) {
            throw CustomQueryNameIsTakenException(query.name)
        }
        customQueryCollection.insertOne(query)
    }
}