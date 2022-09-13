package nl.knaw.huc.annorepo.auth

import io.dropwizard.auth.Authenticator
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType
import io.swagger.v3.oas.annotations.security.SecurityScheme
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

@SecurityScheme(type = SecuritySchemeType.HTTP, name = "Bearer", `in` = SecuritySchemeIn.HEADER, scheme = "Bearer")
class AROAuthAuthenticator(private val userDAO: UserDAO) : Authenticator<String, User> {
    val log: Logger = LoggerFactory.getLogger(javaClass)

    override fun authenticate(apiKey: String?): Optional<User> =
        Optional.ofNullable(userDAO.userForApiKey(apiKey))

}

