package nl.knaw.huc.annorepo.resources.tools

import com.mongodb.client.MongoClient
import com.mongodb.client.MongoDatabase
import org.bson.conversions.Bson
import org.slf4j.LoggerFactory
import nl.knaw.huc.annorepo.api.SearchTaskIndex
import nl.knaw.huc.annorepo.config.AnnoRepoConfiguration
import nl.knaw.huc.annorepo.service.UriFactory

class SearchManager(
    configuration: AnnoRepoConfiguration,
    client: MongoClient
) {
    data class Context(val uriFactory: UriFactory, val mdb: MongoDatabase)

    val context = Context(
        uriFactory = UriFactory(configuration),
        mdb = client.getDatabase(configuration.databaseName)
    )

    private val log = LoggerFactory.getLogger(javaClass)

    fun startGlobalSearch(
        containerNames: List<String>,
        queryMap: HashMap<*, *>,
        aggregateStages: List<Bson>
    ): SearchTask =
        startSearchTask(
            GlobalSearchTask(
                containerNames = containerNames,
                queryMap = queryMap,
                aggregateStages = aggregateStages,
                context = context
            )
        )

    fun getSearchTask(id: String): SearchTask? = SearchTaskIndex[id]

    private fun startSearchTask(task: SearchTask): SearchTask {
        SearchTaskIndex[task.id] = task
        Thread(task).start()
        return task
    }

}