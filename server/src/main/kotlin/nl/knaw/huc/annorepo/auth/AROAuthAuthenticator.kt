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
                { error: SRAMClient.SramTokenError -> logger.debug { "Checking the api-key with the SRAM client failed with: " + error.message }; null },
                { user: SramUser -> logger.debug { "Checking the api-key with the SRAM client succeeded" }; user }
            )
            ?: openIDClients // if we have OpenIdClients, check the apiKey there
                .asSequence()
                .map {
                    val userForToken = it.userForToken(apiKey)
                    val msgPrefix = "Checking the api-key with OIDC client '${it.name}'"
                    userForToken.fold(
                        { e -> logger.debug { "$msgPrefix failed with: ${e.message}" } },
                        { u -> logger.debug { "$msgPrefix succeeded" } }
                    )
                    userForToken
                }
                .firstOrNull { it.isRight() }
                ?.getOrNull()
        logger.debug { "api-key matches user ${userForApiKey ?: "anonymous"}" }
        return Optional.ofNullable(userForApiKey)
    }

}
