package nl.knaw.huc.annorepo.api

import java.util.Date
import nl.knaw.huc.annorepo.resources.tools.SearchTask

object SearchTaskIndex {

    private val index: MutableMap<String, SearchTask> = mutableMapOf()

    operator fun get(id: String): SearchTask? = index[id]
    operator fun set(id: String, value: SearchTask) {
        index[id] = value
    }

    fun purgeExpiredTasks() {
        val expiredTaskIds = index.entries
            .asSequence()
            .filter { it.value.status.state == SearchTask.State.DONE }
            .filter { it.value.status.expirationTime()!!.before(Date()) }
            .map { it.key }
            .toList()
        expiredTaskIds
            .forEach { index.remove(it) }
    }

}