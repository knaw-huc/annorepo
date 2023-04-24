package nl.knaw.huc.annorepo.resources.tools

import java.util.Date
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoDatabase
import org.bson.conversions.Bson
import org.slf4j.LoggerFactory
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

    private val searchTaskIndex: MutableMap<String, SearchTask> = mutableMapOf()
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

    fun getSearchTask(id: String): SearchTask? = searchTaskIndex[id]

    private fun startSearchTask(task: SearchTask): SearchTask {
        searchTaskIndex[task.id] = task
        Thread(task).start()
        purgeExpiredTasks()
        return task
    }

    private fun purgeExpiredTasks() {
        val expiredTaskIds = searchTaskIndex.entries
            .asSequence()
            .filter { it.value.status.state == SearchTask.State.DONE }
            .filter { it.value.status.expirationTime()!!.before(Date()) }
            .map { it.key }
            .toList()
        log.debug("expired tasks: {}", expiredTaskIds)
        expiredTaskIds
            .forEach { searchTaskIndex.remove(it) }
    }
}