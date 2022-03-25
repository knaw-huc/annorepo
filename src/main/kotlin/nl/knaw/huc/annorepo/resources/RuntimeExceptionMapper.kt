package nl.knaw.huc.annorepo.resources

import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.ext.ExceptionMapper
import javax.ws.rs.ext.Provider

@Provider
class RuntimeExceptionMapper : ExceptionMapper<RuntimeException> {
    override fun toResponse(exception: RuntimeException): Response =
        Response.status(Response.Status.BAD_REQUEST)
            .entity(exception.message)
            .type(MediaType.TEXT_PLAIN)
            .build()
}
