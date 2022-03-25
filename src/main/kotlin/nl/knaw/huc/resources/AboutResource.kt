package nl.knaw.huc.resources

import com.codahale.metrics.annotation.Timed
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import nl.knaw.huc.AnnoRepoConfiguration
import nl.knaw.huc.api.AboutInfo
import nl.knaw.huc.api.ResourcePaths
import java.time.Instant
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(ResourcePaths.ABOUT)
@Path(ResourcePaths.ABOUT)
@Produces(MediaType.APPLICATION_JSON)
class AboutResource(configuration: AnnoRepoConfiguration, appName: String) {
    @get:ApiOperation(value = "Get some info about the server", response = AboutInfo::class)
    @get:Timed
    @get:GET
    val about = AboutInfo()

    init {
        about.appName = appName
        about.startedAt = Instant.now().toString()
    }
}