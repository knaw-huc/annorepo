package nl.knaw.huc.annorepo.resources.tools

import org.bson.conversions.Bson
import org.slf4j.LoggerFactory

class ContainerSearchChore(
    private val containerName: String,
    private val queryMap: HashMap<*, *>,
    private val aggregateStages: List<Bson>
) :
    SearchChore(queryMap) {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun runSearch(status: Status) {
        log.debug("containerName={},query={}", containerName, queryMap)
        Thread.sleep(1000L)
    }
}