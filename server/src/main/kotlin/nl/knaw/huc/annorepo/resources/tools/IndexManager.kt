package nl.knaw.huc.annorepo.resources.tools

import com.mongodb.client.MongoClient
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import org.bson.Document
import org.bson.conversions.Bson
import org.slf4j.LoggerFactory
import nl.knaw.huc.annorepo.api.IndexChoreIndex
import nl.knaw.huc.annorepo.config.AnnoRepoConfiguration
import nl.knaw.huc.annorepo.service.UriFactory

class IndexManager(
    configuration: AnnoRepoConfiguration,
    client: MongoClient
) {
    data class Context(val uriFactory: UriFactory, val mdb: MongoDatabase)

    val context = Context(
        uriFactory = UriFactory(configuration),
        mdb = client.getDatabase(configuration.databaseName)
    )

    private val log = LoggerFactory.getLogger(javaClass)

    fun startIndexCreation(
        container: MongoCollection<Document>,
        fieldName: String,
        index: Bson
    ): IndexChore =
        startIndexChore(
            IndexChore(
                container = container,
                fieldName = fieldName,
                index = index
            )
        )

    fun getIndexChore(id: String): IndexChore? = IndexChoreIndex[id]

    private fun startIndexChore(chore: IndexChore): IndexChore {
        IndexChoreIndex[chore.id] = chore
        Thread(chore).start()
        return chore
    }

}