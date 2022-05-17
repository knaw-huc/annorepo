package nl.knaw.huc.annorepo

import co.elastic.clients.elasticsearch.ElasticsearchClient
import co.elastic.clients.json.jackson.JacksonJsonpMapper
import co.elastic.clients.transport.ElasticsearchTransport
import co.elastic.clients.transport.rest_client.RestClientTransport
import com.codahale.metrics.health.HealthCheck
import com.fasterxml.jackson.databind.module.SimpleModule
import io.dropwizard.Application
import io.dropwizard.configuration.EnvironmentVariableSubstitutor
import io.dropwizard.configuration.SubstitutingSourceProvider
import io.dropwizard.jdbi3.JdbiFactory
import io.dropwizard.jdbi3.bundles.JdbiExceptionsBundle
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import io.federecio.dropwizard.swagger.SwaggerBundle
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration
import nl.knaw.huc.annorepo.api.ARConst
import nl.knaw.huc.annorepo.api.ElasticsearchWrapper
import nl.knaw.huc.annorepo.cli.EnvCommand
import nl.knaw.huc.annorepo.config.AnnoRepoConfiguration
import nl.knaw.huc.annorepo.health.ServerHealthCheck
import nl.knaw.huc.annorepo.resources.AboutResource
import nl.knaw.huc.annorepo.resources.BatchResource
import nl.knaw.huc.annorepo.resources.HomePageResource
import nl.knaw.huc.annorepo.resources.ListResource
import nl.knaw.huc.annorepo.resources.RuntimeExceptionMapper
import nl.knaw.huc.annorepo.resources.SearchResource
import nl.knaw.huc.annorepo.resources.W3CResource
import nl.knaw.huc.annorepo.service.LocalDateTimeSerializer
import org.apache.commons.lang3.StringUtils
import org.apache.http.HttpHost
import org.elasticsearch.client.RestClient
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.postgres.PostgresPlugin
import org.jdbi.v3.sqlobject.SqlObjectPlugin
import org.slf4j.LoggerFactory
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicBoolean

class AnnoRepoApplication : Application<AnnoRepoConfiguration?>() {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun getName(): String = "AnnoRepo"

    override fun initialize(bootstrap: Bootstrap<AnnoRepoConfiguration?>) {
        bootstrap.configurationSourceProvider = SubstitutingSourceProvider(
            bootstrap.configurationSourceProvider, EnvironmentVariableSubstitutor()
        )
        bootstrap.addBundle(getSwaggerBundle())
        bootstrap.addBundle(JdbiExceptionsBundle())
        bootstrap.addCommand(EnvCommand())
    }

    private fun getSwaggerBundle() = object : SwaggerBundle<AnnoRepoConfiguration>() {
        override fun getSwaggerBundleConfiguration(configuration: AnnoRepoConfiguration): SwaggerBundleConfiguration =
            configuration.swaggerBundleConfiguration
    }

    override fun run(configuration: AnnoRepoConfiguration?, environment: Environment) {
        log.info(
            "AR_ environment variables:\n\n" +
                    ARConst.EnvironmentVariable.values()
                        .joinToString("\n") { e ->
                            "  ${e.name}:\t${System.getenv(e.name) ?: "(not set, using default)"}"
                        } +
                    "\n"
        )
        log.info("db.url = {}", configuration!!.database.url)
        log.info("db.user = {}", configuration.database.user)
        log.info("db.password = {}", configuration.database.password)

        log.info("es.host = {}", configuration.elasticSearchConfiguration.host)
        log.info("es.port = {}", configuration.elasticSearchConfiguration.port)

        val jdbi = createJdbi(environment, configuration)
        val esClient = createESClient(configuration)
        val esWrapper = ElasticsearchWrapper(esClient)

        val appVersion = javaClass.getPackage().implementationVersion
        environment.jersey().apply {
            register(AboutResource(configuration, name, appVersion))
            register(HomePageResource())
            register(W3CResource(configuration, jdbi, esWrapper))
            register(SearchResource(configuration, jdbi, esClient))
            register(BatchResource(configuration, jdbi, esWrapper))
            register(ListResource(configuration, jdbi))
            register(RuntimeExceptionMapper())
        }
        environment.healthChecks().apply {
            register("server", ServerHealthCheck())
        }

        customizeObjectMapper(environment)

        doHealthChecks(environment)

        log.info(
            "\n\n  Starting $name (v$appVersion)\n" +
                    "    locally accessible at    http://localhost:${System.getenv(ARConst.EnvironmentVariable.AR_SERVER_PORT.name) ?: 8080}\n" +
                    "    externally accessible at ${configuration.externalBaseUrl}\n"
        )
    }

    private fun createESClient(configuration: AnnoRepoConfiguration): ElasticsearchClient {
        val restClient = RestClient.builder(
            HttpHost(
                configuration.elasticSearchConfiguration.host,
                configuration.elasticSearchConfiguration.port
            )
        ).build()
        val transport: ElasticsearchTransport = RestClientTransport(restClient, JacksonJsonpMapper())
        return ElasticsearchClient(transport)
    }

    private fun customizeObjectMapper(environment: Environment) {
        val objectMapper = environment.objectMapper
        objectMapper.dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
        val module = SimpleModule()
        module.addSerializer(LocalDateTime::class.java, LocalDateTimeSerializer("yyyy-MM-dd'T'HH:mm:ss"))
        objectMapper.registerModule(module)
    }

    private fun createJdbi(
        environment: Environment,
        configuration: AnnoRepoConfiguration
    ): Jdbi {
        val factory = JdbiFactory()
        val jdbi = factory.build(environment, configuration.database, "postgresql")
        jdbi.installPlugin(SqlObjectPlugin())
        jdbi.installPlugin(PostgresPlugin())
        return jdbi
    }

    private fun doHealthChecks(environment: Environment) {
        val results = environment.healthChecks().runHealthChecks()
        val healthy = AtomicBoolean(true)
        log.info("Health checks:")
        results.forEach { (name: String?, result: HealthCheck.Result) ->
            log.info(
                "  {}: {}, message='{}'",
                name,
                if (result.isHealthy) "healthy" else "unhealthy",
                StringUtils.defaultIfBlank(result.message, "")
            )
            healthy.set(healthy.get() && result.isHealthy)
        }
        if (!healthy.get()) {
            throw RuntimeException("Failing health check(s)")
        }
    }

    companion object {
        @Throws(Exception::class)
        @JvmStatic
        fun main(args: Array<String>) {
            AnnoRepoApplication().run(*args)
        }
    }
}