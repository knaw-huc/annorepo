package nl.knaw.huc.annorepo.resources.tools

import org.bson.Document
import org.bson.conversions.Bson
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class GlobalSearchChore(
    private val containerNames: List<String>,
    queryMap: HashMap<*, *>,
    private val aggregateStages: List<Bson>,
    private val context: SearchManager.Context
) : SearchChore(queryMap) {
    val log: Logger = LoggerFactory.getLogger(this.javaClass)
    override fun runSearch(status: Status) {
        status.totalContainersToSearch = containerNames.size
        for (containerName in containerNames) {
            context.mdb.getCollection(containerName)
                .aggregate(aggregateStages)
                .map { a -> toMongoDocumentId(a, containerName) }
                .forEach(status.annotationIds::add)
            status.containersSearched.incrementAndGet()
        }
    }

    private fun toMongoDocumentId(a: Document, containerName: String): MongoDocumentId {
        return MongoDocumentId(containerName, a.getObjectId("_id"))
    }
}

