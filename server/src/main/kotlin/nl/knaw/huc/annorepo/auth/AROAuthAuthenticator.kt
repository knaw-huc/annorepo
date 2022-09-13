package nl.knaw.huc.annorepo.auth

import io.dropwizard.auth.Authenticator
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType
import io.swagger.v3.oas.annotations.security.SecurityScheme
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    `in` = SecuritySchemeIn.HEADER,
)
class AROAuthAuthenticator(private val userDAO: UserDAO) : Authenticator<String, User> {
    val log: Logger = LoggerFactory.getLogger(javaClass)

    override fun authenticate(apiKey: String?): Optional<User> =
        Optional.ofNullable(userDAO.userForApiKey(apiKey))

}

