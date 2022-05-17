package nl.knaw.huc.annorepo.resources

import com.codahale.metrics.annotation.Timed
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import nl.knaw.huc.annorepo.api.AboutInfo
import nl.knaw.huc.annorepo.api.ResourcePaths
import nl.knaw.huc.annorepo.config.AnnoRepoConfiguration
import org.slf4j.LoggerFactory
import java.time.Instant
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(ResourcePaths.ABOUT)
@Path(ResourcePaths.ABOUT)
@Produces(MediaType.APPLICATION_JSON)
class AboutResource(configuration: AnnoRepoConfiguration, appName: String, version: String) {

    private val log = LoggerFactory.getLogger(javaClass)

    private val about = AboutInfo(
            appName = appName,
            version = version,
            startedAt = Instant.now().toString(),
            baseURI = configuration.externalBaseUrl
    )

    @ApiOperation(value = "Get some info about the server", response = AboutInfo::class)
    @Timed
    @GET
    fun getAboutInfo(): AboutInfo = about

}