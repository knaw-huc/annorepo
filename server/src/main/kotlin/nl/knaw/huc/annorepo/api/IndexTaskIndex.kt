package nl.knaw.huc.annorepo.api

import java.util.Date
import nl.knaw.huc.annorepo.resources.tools.IndexTask

object IndexTaskIndex {

    private val index: MutableMap<String, IndexTask> = mutableMapOf()

    operator fun get(id: String): IndexTask? = index[id]
    operator fun set(id: String, value: IndexTask) {
        index[id] = value
    }

    fun purgeExpiredTasks() {
        val expiredTaskIds = index.entries
            .asSequence()
            .filter { it.value.status.state == IndexTask.State.DONE }
            .filter { it.value.status.expirationTime()!!.before(Date()) }
            .map { it.key }
            .toList()
        expiredTaskIds
            .forEach { index.remove(it) }
    }

}