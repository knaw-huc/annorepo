package nl.knaw.huc;

import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

public class AnnoRepoApplication extends Application<AnnoRepoConfiguration> {

    public static void main(final String[] args) throws Exception {
        new AnnoRepoApplication().run(args);
    }

    @Override
    public String getName() {
        return "AnnoRepo";
    }

    @Override
    public void initialize(final Bootstrap<AnnoRepoConfiguration> bootstrap) {
        // TODO: application initialization
    }

    @Override
    public void run(final AnnoRepoConfiguration configuration,
                    final Environment environment) {
        // TODO: implement application
    }

}
