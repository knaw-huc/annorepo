package nl.knaw.huc.annorepo.auth

import javax.ws.rs.BadRequestException
import javax.ws.rs.container.ContainerRequestContext
import javax.ws.rs.container.ContainerRequestFilter
import javax.ws.rs.core.Response

@NeedsAPIKey
class AuthenticateFilter : ContainerRequestFilter {

//    val log: Logger = LoggerFactory.getLogger(javaClass)

    override fun filter(context: ContainerRequestContext) {
        val apiKeys: MutableList<String> = context.headers[API_KEY_HEADER]
            ?: throw BadRequestException("Missing header: ${nl.knaw.huc.annorepo.resources.API_KEY_HEADER}")
        val authenticationIsValid = authenticationIsValid(apiKeys[0])
        if (!authenticationIsValid) {
            context.abortWith(Response.status(Response.Status.UNAUTHORIZED).entity("Not authorized").build())
        }
    }

    private fun authenticationIsValid(apiKey: String): Boolean =
        apiKey != "no"

    companion object {
        private const val API_KEY_HEADER = "X-API-Key"
    }
}