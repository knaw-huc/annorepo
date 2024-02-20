package nl.knaw.huc.annorepo.resources.tools

import java.util.Date
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import org.apache.logging.log4j.kotlin.logger
import org.joda.time.Instant
import nl.knaw.huc.annorepo.api.SearchStatusSummary

abstract class SearchChore(queryMap: HashMap<*, *>) : Runnable {

    class Status(private val queryMap: HashMap<*, *>) {

        var state = State.CREATED
        val annotationIds: MutableList<MongoDocumentId> = mutableListOf()
        var startTime: Instant = Instant.now()
        var endTime: Instant? = null
        var totalContainersToSearch: Int = 0
        val containersSearched: AtomicInteger = AtomicInteger(0)
        val errors: MutableList<String> = mutableListOf()

        fun summary(): SearchStatusSummary = SearchStatusSummary(
            query = queryMap,
            startedAt = startTime.toDate(),
            finishedAt = endTime?.toDate(),
            expiresAfter = expirationTime(),
            state = state.name,
            totalContainersToSearch = totalContainersToSearch,
            containersSearched = containersSearched.get(),
            hitsFoundSoFar = annotationIds.size,
            errors = errors,
            processingTimeInMillis = (endTime?.millis ?: Instant.now().millis) - startTime.millis
        )

        private val timeToLive = TimeUnit.HOURS.toMillis(1)

        fun expirationTime(): Date? = endTime?.withDurationAdded(timeToLive, 1)?.toDate()
    }

    enum class State {
        CREATED, RUNNING, DONE, FAILED
    }

    val id: String = UUID.randomUUID().toString()
    val status = Status(queryMap)

    override fun run() {
        status.state = State.RUNNING
        status.startTime = Instant.now()
        try {
            runSearch(status)
            status.state = State.DONE
        } catch (t: Throwable) {
            t.printStackTrace()
            status.state = State.FAILED
            status.errors += "${t.javaClass}: ${t.message ?: ""}"
        }
        status.endTime = Instant.now()
        logger.debug { "query done" }
    }

    abstract fun runSearch(status: Status)
}

