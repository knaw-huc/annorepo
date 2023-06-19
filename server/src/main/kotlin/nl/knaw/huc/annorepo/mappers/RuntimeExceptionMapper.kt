package nl.knaw.huc.annorepo.mappers

import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.ExceptionMapper
import jakarta.ws.rs.ext.Provider

@Provider
class RuntimeExceptionMapper : ExceptionMapper<RuntimeException> {
    override fun toResponse(exception: RuntimeException): Response =
        Response.status(Response.Status.BAD_REQUEST)
            .entity(exception.message)
            .type(MediaType.TEXT_PLAIN)
            .build()
}
