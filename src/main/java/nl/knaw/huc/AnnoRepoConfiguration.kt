package nl.knaw.huc;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration;

import nl.knaw.huc.resources.AboutResource;

public class AnnoRepoConfiguration extends Configuration {
  @JsonProperty("swagger")
  public final SwaggerBundleConfiguration swaggerBundleConfiguration =
      new SwaggerBundleConfiguration();

  AnnoRepoConfiguration() {
    super();
    setDefaults();
  }

  private void setDefaults() {
    String name = AboutResource.class.getPackage().getName();
    swaggerBundleConfiguration.setResourcePackage(name);
  }
}
