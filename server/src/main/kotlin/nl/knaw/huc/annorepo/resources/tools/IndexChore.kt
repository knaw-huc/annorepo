package nl.knaw.huc.annorepo.resources.tools

import java.util.Date
import java.util.concurrent.TimeUnit
import com.mongodb.client.MongoCollection
import org.apache.logging.log4j.kotlin.logger
import org.bson.Document
import org.bson.conversions.Bson
import org.joda.time.Instant
import nl.knaw.huc.annorepo.api.ChoreStatusSummary
import nl.knaw.huc.annorepo.dao.ContainerDAO

class IndexChore(
    val id: String,
    private val container: MongoCollection<Document>,
    private val containerName: String,
    private val fieldNames: List<String>,
    private val index: Bson,
    private val containerDAO: ContainerDAO
) :
    Runnable {

    class Status {
        var state = State.CREATED
        var startTime: Instant = Instant.now()
        var endTime: Instant? = null
        val errors: MutableList<String> = mutableListOf()

        fun summary(): ChoreStatusSummary = ChoreStatusSummary(
            startedAt = startTime.toDate(),
            finishedAt = endTime?.toDate(),
            expiresAfter = expirationTime(),
            state = state.name,
            errors = errors,
            processingTimeInMillis = (endTime?.millis ?: Instant.now().millis) - startTime.millis
        )

        private val timeToLive = TimeUnit.HOURS.toMillis(1)

        fun expirationTime(): Date? = endTime?.withDurationAdded(timeToLive, 1)?.toDate()
    }

    enum class State {
        CREATED, RUNNING, DONE, FAILED
    }

    val status = Status()

    override fun run() {
        status.state = State.RUNNING
        status.startTime = Instant.now()
        try {
            val indexName = container.createIndex(index)
//            val partialFilter = Filters.and(fieldNames.map { Filters.exists(it) })
//            val indexName = container.createIndex(index, IndexOptions().partialFilterExpression(partialFilter))
            logger.info { "created index: $indexName" }
            val metadata = containerDAO.getContainerMetadata(containerName)
                ?: throw RuntimeException("no metatadata found for $containerName")
            metadata.indexMap[id] = indexName
            containerDAO.updateContainerMetadata(containerName, metadata, false)
            status.state = State.DONE
        } catch (t: Throwable) {
            t.printStackTrace()
            status.state = State.FAILED
            status.errors += "${t.javaClass}: ${t.message ?: ""}"
        }
        status.endTime = Instant.now()
        logger.debug { "query done" }
    }

}