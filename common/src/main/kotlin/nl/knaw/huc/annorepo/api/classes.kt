package nl.knaw.huc.annorepo.api

data class RejectedUserEntry(val userEntry: Map<String, String>, val reason: String)
data class UserAddResults(val added: List<String>, val rejected: List<RejectedUserEntry>)
