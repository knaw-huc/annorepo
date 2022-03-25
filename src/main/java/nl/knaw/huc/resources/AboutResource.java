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
  private static final String PROPERTIES_FILE = "about.properties";

  private final AboutInfo about = new AboutInfo();

  public AboutResource(AnnoRepoConfiguration configuration, String appName) {
    //        PropertiesConfiguration properties = new PropertiesConfiguration(PROPERTIES_FILE,
    // true);
    about.setAppName(appName);
    about.setStartedAt(Instant.now().toString());
    //            about.setBuildDate(
    //                properties.getProperty("buildDate").orElse("no buildDate set in
    // about.properties"));
    //            about.setCommitId(
    //                properties.getProperty("commitId").orElse("no commitId set in
    // about.properties"));
    //            about..setScmBranch(
    //                properties.getProperty("scmBranch").orElse("no scmBranch set in
    // about.properties"))
    //            about.setVersion(properties.getProperty("version").orElse("no version set in
    // about.properties"));
  }

  @GET
  @Timed
  @ApiOperation(value = "Get some info about the server", response = AboutInfo.class)
  public AboutInfo getAbout() {
    return about;
  }
}
