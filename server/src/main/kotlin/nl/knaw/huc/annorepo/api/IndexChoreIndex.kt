package nl.knaw.huc.annorepo.api

import java.util.Date
import nl.knaw.huc.annorepo.resources.tools.IndexChore

object IndexChoreIndex {

    private val index: MutableMap<String, IndexChore> = mutableMapOf()

    operator fun get(id: String): IndexChore? = index[id]
    operator fun set(id: String, value: IndexChore) {
        index[id] = value
    }

    fun purgeExpiredChores() {
        val expiredChoreIds = index.entries
            .asSequence()
            .filter { it.value.status.state == IndexChore.State.DONE }
            .filter { it.value.status.expirationTime()!!.before(Date()) }
            .map { it.key }
            .toList()
        expiredChoreIds
            .forEach { index.remove(it) }
    }

}