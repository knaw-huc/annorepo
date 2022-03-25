package nl.knaw.huc.annorepo.annorepo

import io.dropwizard.Application
import io.dropwizard.configuration.EnvironmentVariableSubstitutor
import io.dropwizard.configuration.SubstitutingSourceProvider
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import io.federecio.dropwizard.swagger.SwaggerBundle
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration
import nl.knaw.huc.annorepo.AnnoRepoConfiguration
import nl.knaw.huc.annorepo.resources.AboutResource
import nl.knaw.huc.annorepo.resources.HomePageResource
import org.slf4j.LoggerFactory

class AnnoRepoApplication : Application<AnnoRepoConfiguration?>() {
    private val LOG = LoggerFactory.getLogger(javaClass)

    override fun getName(): String {
        return "AnnoRepo"
    }

    override fun initialize(bootstrap: Bootstrap<AnnoRepoConfiguration?>) {
        bootstrap.configurationSourceProvider = SubstitutingSourceProvider(
            bootstrap.configurationSourceProvider, EnvironmentVariableSubstitutor()
        )
        bootstrap.addBundle(
            object : SwaggerBundle<AnnoRepoConfiguration>() {
                override fun getSwaggerBundleConfiguration(
                    configuration: AnnoRepoConfiguration
                ): SwaggerBundleConfiguration {
                    return configuration.swaggerBundleConfiguration
                }
            })
    }

    override fun run(configuration: AnnoRepoConfiguration?, environment: Environment) {
        environment.jersey().register(AboutResource(configuration!!, name))
        environment.jersey().register(HomePageResource())
    }

    companion object {
        @Throws(Exception::class)
        @JvmStatic
        fun main(args: Array<String>) {
            AnnoRepoApplication().run(*args)
        }
    }
}