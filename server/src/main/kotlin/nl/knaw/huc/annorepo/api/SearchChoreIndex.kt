package nl.knaw.huc.annorepo.api

import java.util.Date
import nl.knaw.huc.annorepo.resources.tools.SearchChore

object SearchChoreIndex {

    private val index: MutableMap<String, SearchChore> = mutableMapOf()

    operator fun get(id: String): SearchChore? = index[id]
    operator fun set(id: String, value: SearchChore) {
        index[id] = value
    }

    fun purgeExpiredChores() {
        val expiredChoresIds = index.entries
            .asSequence()
            .filter { it.value.status.state == SearchChore.State.DONE }
            .filter { it.value.status.expirationTime()!!.before(Date()) }
            .map { it.key }
            .toList()
        expiredChoresIds
            .forEach { index.remove(it) }
    }

}