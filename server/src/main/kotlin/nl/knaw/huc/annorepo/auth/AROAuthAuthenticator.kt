package nl.knaw.huc.annorepo.auth

import java.util.Optional
import io.dropwizard.auth.Authenticator
import org.apache.logging.log4j.kotlin.logger
import nl.knaw.huc.annorepo.dao.UserDAO

class AROAuthAuthenticator(
    private val userDAO: UserDAO,
    private val sramClient: SRAMClient? = null,
    private val openIDClients: List<OpenIDClient> = listOf()
) : Authenticator<String, User> {

    override fun authenticate(apiKey: String?): Optional<User> {
        logger.debug { "Received api-key $apiKey" }
        val userForApiKey = userDAO.userForApiKey(apiKey) // check the internal db first for the api-key
            ?: sramClient?.userForToken(apiKey)?.fold( // if we have an SRAMClient, check the apiKey there
                { error: SRAMClient.SramTokenError -> /*logger.warn { error.message };*/ null },
                { user: SramUser -> user }
            )
            ?: openIDClients // if we have OpenIdClients, check the apiKey there
                .asSequence()
                .map { it.userForToken(apiKey) }
                .firstOrNull { it.isRight() }
                ?.fold(
                    { error: OpenIDClient.OpenIDTokenError -> /*logger.warn { error.message };*/ null },
                    { user: OpenIDUser -> user }
                )
        logger.debug { "api-key matches user $userForApiKey" }
        return Optional.ofNullable(userForApiKey)
    }

}
