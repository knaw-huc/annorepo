package nl.knaw.huc.annorepo.resources

import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import com.codahale.metrics.annotation.Timed
import io.swagger.v3.oas.annotations.Operation
import nl.knaw.huc.annorepo.api.ResourcePaths
import nl.knaw.huc.annorepo.config.AnnoRepoConfiguration

@Path(ResourcePaths.GRPC)
@Produces(MediaType.APPLICATION_JSON)
class GrpcResource(configuration: AnnoRepoConfiguration) {

    @Operation(description = "bla")
    @Timed
    @GET
    fun getAboutInfo(): String = "about"

}