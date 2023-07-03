package nl.knaw.huc.annorepo.resources.tools

import com.mongodb.client.MongoClient
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import org.bson.Document
import org.bson.conversions.Bson
import org.slf4j.LoggerFactory
import nl.knaw.huc.annorepo.api.IndexTaskIndex
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
    ): IndexTask =
        startIndexTask(
            IndexTask(
                container = container,
                fieldName = fieldName,
                index = index
            )
        )

    fun getIndexTask(id: String): IndexTask? = IndexTaskIndex[id]

    private fun startIndexTask(task: IndexTask): IndexTask {
        IndexTaskIndex[task.id] = task
        Thread(task).start()
        return task
    }

}