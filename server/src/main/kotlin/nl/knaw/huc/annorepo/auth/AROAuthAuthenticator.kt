package nl.knaw.huc.annorepo.auth

import java.util.Optional
import io.dropwizard.auth.Authenticator
import org.apache.logging.log4j.kotlin.logger
import nl.knaw.huc.annorepo.dao.UserDAO

class AROAuthAuthenticator(private val userDAO: UserDAO) : Authenticator<String, User> {

    override fun authenticate(apiKey: String?): Optional<User> {
        logger.debug { "Received api-key $apiKey" }
        val userForApiKey = userDAO.userForApiKey(apiKey)
        logger.debug { "api-key matches user $userForApiKey" }
        return Optional.ofNullable(userForApiKey)
    }

}

