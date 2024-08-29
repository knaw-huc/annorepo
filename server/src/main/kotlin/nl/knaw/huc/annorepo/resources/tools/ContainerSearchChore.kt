package nl.knaw.huc.annorepo.resources.tools

import org.apache.logging.log4j.kotlin.logger
import org.bson.conversions.Bson

class ContainerSearchChore(
    private val containerName: String,
    private val queryMap: HashMap<*, *>,
    private val aggregateStages: List<Bson>
) :
    SearchChore(queryMap) {

    override fun runSearch(status: Status) {
        logger.debug { "containerName=$containerName,query=$queryMap" }
        Thread.sleep(1000L)
    }
}