package nl.knaw.huc.annorepo.auth

import java.util.Optional
import io.dropwizard.auth.Authenticator
import org.apache.logging.log4j.kotlin.logger
import nl.knaw.huc.annorepo.dao.UserDAO

class AROAuthAuthenticator(
    private val userDAO: UserDAO,
    private val sramClient: SRAMClient? = null,
    private val openIDClient: OpenIDClient? = null
) : Authenticator<String, User> {

    override fun authenticate(apiKey: String?): Optional<User> {
        logger.debug { "Received api-key $apiKey" }
        val userForApiKey = userDAO.userForApiKey(apiKey)
            ?: sramClient?.userForToken(apiKey)?.fold(
                { error: SRAMClient.SramTokenError -> logger.info { error.message }; null },
                { user: SramUser -> user }
            )
            ?: openIDClient?.userForToken(apiKey)?.fold(
                { error: OpenIDClient.OpenIDTokenError -> logger.info { error.message }; null },
                { user: OpenIDUser -> user })
        logger.debug { "api-key matches user $userForApiKey" }
        return Optional.ofNullable(userForApiKey)
    }

}
