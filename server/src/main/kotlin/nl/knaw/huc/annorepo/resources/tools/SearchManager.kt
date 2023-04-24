package nl.knaw.huc.annorepo.resources.tools

import com.mongodb.client.MongoClient
import org.bson.conversions.Bson
import nl.knaw.huc.annorepo.config.AnnoRepoConfiguration

object SearchManager {

    private val searchTaskIndex: MutableMap<String, SearchTask> = mutableMapOf()

    fun startContainerSearch(
        containerName: String,
        queryMap: HashMap<*, *>,
        aggregateStages: List<Bson>
    ): SearchTask =
        startSearchTask(ContainerSearchTask(containerName, queryMap, aggregateStages))

    fun startGlobalSearch(
        containerNames: List<String>,
        queryMap: HashMap<*, *>,
        aggregateStages: List<Bson>,
        configuration: AnnoRepoConfiguration,
        client: MongoClient
    ): SearchTask =
        startSearchTask(GlobalSearchTask(containerNames, queryMap, aggregateStages, configuration, client))

    fun getSearchTask(id: String): SearchTask? = searchTaskIndex[id]

    private fun startSearchTask(task: SearchTask): SearchTask {
        searchTaskIndex[task.id] = task
        Thread(task).start()
        return task
    }
}