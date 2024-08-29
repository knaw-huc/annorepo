package nl.knaw.huc.annorepo.resources

import java.time.Instant
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import com.codahale.metrics.annotation.Timed
import io.swagger.v3.oas.annotations.Operation
import nl.knaw.huc.annorepo.api.AboutInfo
import nl.knaw.huc.annorepo.api.ResourcePaths
import nl.knaw.huc.annorepo.config.AnnoRepoConfiguration

@Path(ResourcePaths.ABOUT)
@Produces(MediaType.APPLICATION_JSON)
class AboutResource(configuration: AnnoRepoConfiguration, appName: String, version: String, mongoVersion: String) {

    private val about = AboutInfo(
        appName = appName,
        version = version,
        startedAt = Instant.now().toString(),
        baseURI = configuration.externalBaseUrl,
        withAuthentication = configuration.withAuthentication,
        mongoVersion = mongoVersion
//        grpcHostName = configuration.grpc.hostName,
//        grpcPort = configuration.grpc.port
    )

    @Operation(description = "Get some info about the server")
    @Timed
    @GET
    fun getAboutInfo(): AboutInfo = about

}