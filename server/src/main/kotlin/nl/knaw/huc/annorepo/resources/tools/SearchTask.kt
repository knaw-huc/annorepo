package nl.knaw.huc.annorepo.resources.tools

import java.util.Date
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import org.joda.time.Instant
import org.slf4j.LoggerFactory

abstract class SearchTask(queryMap: HashMap<*, *>) : Runnable {

    class Status(private val queryMap: HashMap<*, *>) {

        data class Summary(
            val query: HashMap<*, *>,
            val startedAt: Date,
            val finishedAt: Date?,
            val state: String,
            val containersSearched: Int,
            val totalContainersToSearch: Int,
            val hitsFoundSoFar: Int,
            val processingTimeInMillis: Long
        )

        var state = SearchTaskState.CREATED
        val annotations: MutableList<Map<String, Any>> = mutableListOf()
        lateinit var startTime: Instant
        var endTime: Instant? = null
        var totalContainersToSearch: Int = 0
        val containersSearched: AtomicInteger = AtomicInteger(0)

        fun summary(): Summary = Summary(
            query = queryMap,
            startedAt = startTime.toDate(),
            finishedAt = endTime?.toDate(),
            state = state.name,
            totalContainersToSearch = totalContainersToSearch,
            containersSearched = containersSearched.get(),
            hitsFoundSoFar = annotations.size,
            processingTimeInMillis = (endTime?.millis ?: Instant.now().millis) - startTime.millis
        )

    }

    enum class SearchTaskState {
        CREATED, STARTED, DONE
    }

    private val log = LoggerFactory.getLogger(javaClass)

    val id: String = UUID.randomUUID().toString()
    val status = Status(queryMap)

    override fun run() {
        status.state = SearchTaskState.STARTED
        status.startTime = Instant.now()
        runSearch(status)
        status.state = SearchTaskState.DONE
        status.endTime = Instant.now()
        log.debug("query done")
    }

    abstract fun runSearch(status: Status)
}
