package nl.knaw.huc.resources;

import java.time.Instant;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import nl.knaw.huc.AnnoRepoConfiguration;
import nl.knaw.huc.api.AboutInfo;
import nl.knaw.huc.api.ResourcePaths;

@Api(ResourcePaths.ABOUT)
@Path(ResourcePaths.ABOUT)
@Produces(MediaType.APPLICATION_JSON)
public class AboutResource {
  private final AboutInfo about = new AboutInfo();

  public AboutResource(AnnoRepoConfiguration configuration, String appName) {
    about.setAppName(appName);
    about.setStartedAt(Instant.now().toString());
  }

  @GET
  @Timed
  @ApiOperation(value = "Get some info about the server", response = AboutInfo.class)
  public AboutInfo getAbout() {
    return about;
  }
}
