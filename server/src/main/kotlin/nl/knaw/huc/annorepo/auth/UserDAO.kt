package nl.knaw.huc.annorepo.auth

import nl.knaw.huc.annorepo.api.UserAddResults
import nl.knaw.huc.annorepo.api.UserEntry

interface UserDAO {
    fun userForApiKey(apiKey: String?): User?
    fun addUserEntries(userEntries: List<UserEntry>): UserAddResults
    fun allUserEntries(): List<UserEntry>
    fun deleteUsersByName(userNames: Collection<String>): Boolean
}

