package nl.knaw.huc.annorepo.filters

import java.io.IOException
import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.container.ContainerResponseContext
import jakarta.ws.rs.container.ContainerResponseFilter
import jakarta.ws.rs.ext.Provider

@Provider
class CorsFilter : ContainerResponseFilter {
    @Throws(IOException::class)
    override fun filter(
        requestContext: ContainerRequestContext,
        responseContext: ContainerResponseContext
    ) {
//        val origins = requestContext.headers["Origin"] ?: listOf()
        val exposeHeadersValue = responseContext.headers.keys.map { it.lowercase() }.toList()
            .sorted()
            .joinToString(", ")
        responseContext.headers.add(
            "Access-Control-Allow-Origin", "*"
        )
        responseContext.headers.add(
            "Access-Control-Allow-Credentials", "true"
        )
        responseContext.headers.add(
            "Access-Control-Allow-Methods",
            "GET, POST, PUT, DELETE, OPTIONS, HEAD"
        )
        val allowedHeadersValue =
            DEFAULT_ALLOWED_HEADERS
                .toList()
                .sorted()
                .joinToString(", ")
        responseContext.headers.add(
            "Access-Control-Allow-Headers",
            allowedHeadersValue
        )
        responseContext.headers.add(
            "Access-Control-Expose-Headers",
            exposeHeadersValue
        )
    }

    companion object {
        val DEFAULT_ALLOWED_HEADERS =
            setOf("origin", "content-type", "accept", "authorization")
    }
}