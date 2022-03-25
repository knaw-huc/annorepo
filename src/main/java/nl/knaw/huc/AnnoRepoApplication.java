package nl.knaw.huc;

import io.dropwizard.Application;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.federecio.dropwizard.swagger.SwaggerBundle;
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.knaw.huc.resources.AboutResource;

public class AnnoRepoApplication extends Application<AnnoRepoConfiguration> {
  private final Logger LOG = LoggerFactory.getLogger(getClass());

  public static void main(final String[] args) throws Exception {
    new AnnoRepoApplication().run(args);
  }

  @Override
  public String getName() {
    return "AnnoRepo";
  }

  @Override
  public void initialize(final Bootstrap<AnnoRepoConfiguration> bootstrap) {
    bootstrap.setConfigurationSourceProvider(
        new SubstitutingSourceProvider(
            bootstrap.getConfigurationSourceProvider(), new EnvironmentVariableSubstitutor()));
    bootstrap.addBundle(
        new SwaggerBundle<AnnoRepoConfiguration>() {
          @Override
          protected SwaggerBundleConfiguration getSwaggerBundleConfiguration(
              AnnoRepoConfiguration configuration) {
            return configuration.swaggerBundleConfiguration;
          }
        });
  }

  @Override
  public void run(final AnnoRepoConfiguration configuration, final Environment environment) {
    environment.jersey().register(new AboutResource(configuration, getName()));
  }
}
