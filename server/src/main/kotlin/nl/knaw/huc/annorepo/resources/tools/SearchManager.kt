package nl.knaw.huc.annorepo.resources.tools

import org.bson.conversions.Bson
import org.slf4j.LoggerFactory
import nl.knaw.huc.annorepo.api.SearchChoreIndex
import nl.knaw.huc.annorepo.config.AnnoRepoConfiguration
import nl.knaw.huc.annorepo.dao.ContainerDAO
import nl.knaw.huc.annorepo.service.UriFactory

class SearchManager(
    configuration: AnnoRepoConfiguration,
    val containerDAO: ContainerDAO
) {
    data class Context(val uriFactory: UriFactory, val containerDAO: ContainerDAO)

    val context = Context(
        uriFactory = UriFactory(configuration),
        containerDAO = containerDAO
    )

    private val log = LoggerFactory.getLogger(javaClass)

    fun startGlobalSearch(
        containerNames: List<String>,
        queryMap: HashMap<*, *>,
        aggregateStages: List<Bson>
    ): SearchChore =
        startSearchChore(
            GlobalSearchChore(
                containerNames = containerNames,
                queryMap = queryMap,
                aggregateStages = aggregateStages,
                context = context
            )
        )

    fun getSearchChore(id: String): SearchChore? = SearchChoreIndex[id]

    fun ping(id: String) = SearchChoreIndex.ping(id)

    private fun startSearchChore(chore: SearchChore): SearchChore {
        SearchChoreIndex[chore.id] = chore
        Thread(chore).start()
        return chore
    }

}