package nl.knaw.huc.annorepo.auth

import io.dropwizard.auth.Authenticator
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

class AROAuthAuthenticator : Authenticator<String, User> {
    val log: Logger = LoggerFactory.getLogger(javaClass)

    override fun authenticate(apiKey: String?): Optional<User> =
        Optional.ofNullable(getUser(apiKey))

    private val apiKey2userName = mapOf(
        "key1" to "user1",
        "key2" to "user2"
    )

    private fun getUser(apiKey: String?): User? {
        log.info("apiKey={}", apiKey)
        val userName = apiKey2userName[apiKey]
        log.info("userName={}", userName)
        return if (userName == null) null else User(userName, Role.GUEST)
    }
}

