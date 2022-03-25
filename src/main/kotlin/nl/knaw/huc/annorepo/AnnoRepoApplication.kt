package nl.knaw.huc.annorepo

import com.codahale.metrics.health.HealthCheck
import io.dropwizard.Application
import io.dropwizard.configuration.EnvironmentVariableSubstitutor
import io.dropwizard.configuration.SubstitutingSourceProvider
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import io.federecio.dropwizard.swagger.SwaggerBundle
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration
import nl.knaw.huc.annorepo.health.ServerHealthCheck
import nl.knaw.huc.annorepo.resources.AboutResource
import nl.knaw.huc.annorepo.resources.HomePageResource
import nl.knaw.huc.annorepo.resources.RuntimeExceptionMapper
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean

class AnnoRepoApplication : Application<AnnoRepoConfiguration?>() {
    private val LOG = LoggerFactory.getLogger(javaClass)

    override fun getName(): String {
        return "AnnoRepo"
    }

    override fun initialize(bootstrap: Bootstrap<AnnoRepoConfiguration?>) {
        bootstrap.configurationSourceProvider = SubstitutingSourceProvider(
            bootstrap.configurationSourceProvider, EnvironmentVariableSubstitutor()
        )
        bootstrap.addBundle(object : SwaggerBundle<AnnoRepoConfiguration>() {
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
        environment.jersey().register(RuntimeExceptionMapper())

        environment.healthChecks().register("server", ServerHealthCheck())


        val results = environment.healthChecks().runHealthChecks()
        val healthy = AtomicBoolean(true)
        LOG.info("Health checks:")
        results.forEach { (name: String?, result: HealthCheck.Result) ->
            LOG.info(
                "{}: {}, message='{}'",
                name,
                if (result.isHealthy) "healthy" else "unhealthy",
                StringUtils.defaultIfBlank(result.message, "")
            )
            healthy.set(healthy.get() && result.isHealthy)
        }
        if (!healthy.get()) {
            throw RuntimeException("Failing health check(s)")
        }
        LOG.info(
            java.lang.String.format(
                "\n\n************************************************************\n** Starting %s at %s **\n************************************************************\n",
                name,
                configuration.getBaseURI()
            )
        )
    }

    companion object {
        @Throws(Exception::class)
        @JvmStatic
        fun main(args: Array<String>) {
            AnnoRepoApplication().run(*args)
        }
    }
}