package nl.knaw.huc.annorepo.resources.tools

import java.util.Date
import java.util.concurrent.TimeUnit
import com.mongodb.client.model.Filters
import com.mongodb.client.model.IndexOptions
import com.mongodb.kotlin.client.MongoCollection
import org.bson.Document
import org.bson.conversions.Bson
import org.joda.time.Instant
import org.slf4j.LoggerFactory
import nl.knaw.huc.annorepo.api.ChoreStatusSummary

class IndexChore(
    val id: String,
    private val container: MongoCollection<Document>,
    private val fieldName: String,
    private val index: Bson
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

    private val log = LoggerFactory.getLogger(javaClass)

    val status = Status()

    override fun run() {
        status.state = State.RUNNING
        status.startTime = Instant.now()
        try {
            val partialFilter = Filters.exists(fieldName)
            val createIndex = container.createIndex(index, IndexOptions().partialFilterExpression(partialFilter))
            log.info("created index: $createIndex")
            status.state = State.DONE
        } catch (t: Throwable) {
            t.printStackTrace()
            status.state = State.FAILED
            status.errors += "${t.javaClass}: ${t.message ?: ""}"
        }
        status.endTime = Instant.now()
        log.debug("query done")
    }

}

