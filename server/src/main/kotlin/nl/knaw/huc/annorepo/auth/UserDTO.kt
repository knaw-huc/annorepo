package nl.knaw.huc.annorepo.auth

interface UserDTO {
    fun userForApiKey(apiKey: String?): User?
}
