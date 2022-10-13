package nl.knaw.huc.annorepo.auth

import io.dropwizard.auth.Authenticator
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

class AROAuthAuthenticator(private val userDAO: UserDAO) : Authenticator<String, User> {
    val log: Logger = LoggerFactory.getLogger(javaClass)

    override fun authenticate(apiKey: String?): Optional<User> {
//        log.debug("Received api-key {}", apiKey)
        val userForApiKey = userDAO.userForApiKey(apiKey)
//        log.debug("api-key matches user {}", userForApiKey)
        return Optional.ofNullable(userForApiKey)
    }

}

