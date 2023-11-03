package nl.knaw.huc.annorepo.auth

import java.security.Principal
import jakarta.ws.rs.WebApplicationException
import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.core.SecurityContext
import io.dropwizard.auth.AuthFilter
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class UnauthenticatedAuthFilter<P> : AuthFilter<P, Principal>() {
    val log: Logger = LoggerFactory.getLogger(javaClass)

//    override fun filter(crc: ContainerRequestContext?) {
//        log.info("ContainerRequestContext={}", crc)
//        log.info("userPrincipal={}", crc!!.securityContext.userPrincipal)
//        log.info("access without credentials")
//    }

    @Throws(WebApplicationException::class)
    override fun filter(requestContext: ContainerRequestContext) {
        // Check if credentials are provided
        if (requestContext.securityContext.userPrincipal != null) {
            // Credentials are present; deny access
            throw WebApplicationException(
                Response.status(Response.Status.FORBIDDEN).entity("Access denied with credentials").build()
            )
        }
        requestContext.securityContext = object : SecurityContext {
            override fun getUserPrincipal(): Principal? {
                return null
            }

            override fun isUserInRole(p0: String?): Boolean {
                return false
            }

            override fun isSecure(): Boolean {
                return true
            }

            override fun getAuthenticationScheme(): String {
                return ""
            }
        }
    }
}
