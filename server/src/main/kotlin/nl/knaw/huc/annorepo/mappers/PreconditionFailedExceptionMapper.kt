package nl.knaw.huc.annorepo.mappers

import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.ExceptionMapper
import jakarta.ws.rs.ext.Provider
import nl.knaw.huc.annorepo.exceptions.PreconditionFailedException

@Provider
class PreconditionFailedExceptionMapper : ExceptionMapper<PreconditionFailedException> {
    override fun toResponse(exception: PreconditionFailedException): Response =
        Response.status(Response.Status.PRECONDITION_FAILED)
            .entity(exception.message)
            .type(MediaType.TEXT_PLAIN)
            .build()
}
