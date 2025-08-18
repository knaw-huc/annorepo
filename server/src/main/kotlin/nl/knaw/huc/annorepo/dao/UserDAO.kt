package nl.knaw.huc.annorepo.dao

import nl.knaw.huc.annorepo.api.UserAddResults
import nl.knaw.huc.annorepo.api.UserEntry
import nl.knaw.huc.annorepo.auth.User

interface UserDAO {
    fun userForApiKey(apiKey: String?): User?
    fun addUserEntries(userEntries: List<UserEntry>): UserAddResults
    fun allUserEntries(): List<UserEntry>
    fun deleteUsersByName(userNames: Collection<String>): Boolean
    fun allGroupNames(): List<String>
}

