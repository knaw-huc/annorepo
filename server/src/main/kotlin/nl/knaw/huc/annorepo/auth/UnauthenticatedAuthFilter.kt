package nl.knaw.huc.annorepo.auth

import java.security.Principal
import jakarta.ws.rs.WebApplicationException
import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.core.SecurityContext
import io.dropwizard.auth.AuthFilter

class UnauthenticatedAuthFilter<P> : AuthFilter<P, Principal>() {

    @Throws(WebApplicationException::class)
    override fun filter(requestContext: ContainerRequestContext) {
        // Check if credentials are provided
        if (requestContext.securityContext.userPrincipal != null) {
            // Credentials are present; deny access
            throw WebApplicationException(
                Response.status(Response.Status.FORBIDDEN)
                    .entity("Access denied with credentials")
                    .build()
            )
        }
        requestContext.securityContext = object : SecurityContext {
            override fun getUserPrincipal(): Principal? = null
            override fun isUserInRole(p0: String?): Boolean = false
            override fun isSecure(): Boolean = true
            override fun getAuthenticationScheme(): String = ""
        }
    }
}
