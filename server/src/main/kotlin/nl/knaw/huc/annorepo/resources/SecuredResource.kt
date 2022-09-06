package nl.knaw.huc.annorepo.resources

import com.codahale.metrics.annotation.Timed
import io.swagger.v3.jaxrs2.integration.OpenApiServlet.APPLICATION_JSON
import io.swagger.v3.oas.annotations.Hidden
import io.swagger.v3.oas.annotations.Operation
import nl.knaw.huc.annorepo.auth.NeedsAPIKey
import nl.knaw.huc.annorepo.config.AnnoRepoConfiguration
import org.slf4j.LoggerFactory
import javax.ws.rs.BadRequestException
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.Path
import javax.ws.rs.Produces

const val API_KEY_HEADER = "X-API-Key"

@Hidden
@Path("secure")
@Produces(APPLICATION_JSON)
class SecuredResource(
    private val configuration: AnnoRepoConfiguration
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Operation(description = "Get in")
    @Timed
    @GET
    fun get(@HeaderParam(API_KEY_HEADER) apiKey: String?): String {
        if (apiKey == null) {
            throw BadRequestException("Missing header: $API_KEY_HEADER")
        }
        val s = "api-key=$apiKey"
        return s
    }

    @Operation(description = "Can i?")
    @Timed
    @GET
    @Path("s")
    @NeedsAPIKey
    fun gets(): String {
        return "yes"
    }

}