package nl.knaw.huc.annorepo.resources

import java.time.Instant.now
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import com.codahale.metrics.annotation.Timed
import io.swagger.v3.oas.annotations.Operation
import nl.knaw.huc.annorepo.api.AboutInfo
import nl.knaw.huc.annorepo.api.ResourcePaths
import nl.knaw.huc.annorepo.config.AnnoRepoConfiguration
import nl.knaw.huc.annorepo.config.AuthenticationConfiguration

@Path(ResourcePaths.ABOUT)
@Produces(MediaType.APPLICATION_JSON)
class AboutResource(
    val configuration: AnnoRepoConfiguration,
    val appName: String,
    val version: String,
    val mongoVersionProducer: () -> String
) {

    @Operation(description = "Get some info about the server")
    @Timed
    @GET
    fun getAboutInfo(): AboutInfo {
        return AboutInfo(
            appName = appName,
            version = version,
            startedAt = startedAt,
            baseURI = configuration.externalBaseUrl,
            withAuthentication = configuration.withAuthentication,
            mongoVersion = mongoVersionProducer.invoke(),
            grpcHostName = configuration.grpc.hostName,
            grpcPort = configuration.grpc.port,
            authentication = authenticationMap(configuration.authentication)
        )
    }

    private fun authenticationMap(authentication: AuthenticationConfiguration?): Map<String, Any> =
        authentication?.toMap() ?: mapOf()

//    @GET
//    @Path("x")
//    fun getExtra(): Response =
//        Response.ok()
//            .header("location", "somewhere")
//            .header("link", "something")
//            .build()

    companion object {
        val startedAt = now().toString()
    }

}