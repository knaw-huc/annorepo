package nl.knaw.huc.annorepo.auth

interface UserDTO {
    fun userForApiKey(apiKey: String?): User?
    fun addUserEntries(userEntries: List<UserEntry>): UserAddResults
    fun allUserEntries(): List<UserEntry>
    fun deleteUsersByName(userNames: Collection<String>): Boolean
}

data class UserEntry(val userName: String, val apiKey: String)
