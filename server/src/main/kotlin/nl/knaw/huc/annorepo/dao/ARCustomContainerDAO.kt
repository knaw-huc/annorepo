package nl.knaw.huc.annorepo.dao

import com.mongodb.client.MongoClient
import nl.knaw.huc.annorepo.api.ARConst
import nl.knaw.huc.annorepo.config.AnnoRepoConfiguration

class ARCustomQueryDAO(configuration: AnnoRepoConfiguration, mongoClient: MongoClient) : CustomQueryDAO {
    private val mdb = mongoClient.getDatabase(configuration.databaseName)
    private val customQueryCollection = mdb.getCollection(ARConst.CUSTOM_QUERY_COLLECTION)

    override fun getAllCustomQueries(): List<CustomQuery> {
        TODO("Not yet implemented")
    }

    override fun getCustomQuery(name: String): CustomQuery {
        TODO("Not yet implemented")
    }

    override fun storeCustomQuery(name: String, query: CustomQuery) {
        TODO("Not yet implemented")
    }
}