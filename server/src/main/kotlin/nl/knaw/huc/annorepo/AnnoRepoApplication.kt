package nl.knaw.huc.annorepo

import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicBoolean
import com.codahale.metrics.health.HealthCheck
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.mongodb.client.MongoClient
import com.mongodb.client.model.Indexes
import io.dropwizard.auth.AuthDynamicFeature
import io.dropwizard.auth.oauth.OAuthCredentialAuthFilter
import io.dropwizard.configuration.EnvironmentVariableSubstitutor
import io.dropwizard.configuration.SubstitutingSourceProvider
import io.dropwizard.core.Application
import io.dropwizard.core.ConfiguredBundle
import io.dropwizard.core.setup.Bootstrap
import io.dropwizard.core.setup.Environment
import io.dropwizard.jdbi3.bundles.JdbiExceptionsBundle
import io.dropwizard.jobs.JobsBundle
import io.federecio.dropwizard.swagger.SwaggerBundle
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration
import org.apache.commons.lang3.StringUtils
import org.litote.kmongo.KMongo
import org.litote.kmongo.getCollection
import org.slf4j.LoggerFactory
import nl.knaw.huc.annorepo.api.ARConst.APP_NAME
import nl.knaw.huc.annorepo.api.ARConst.CONTAINER_METADATA_COLLECTION
import nl.knaw.huc.annorepo.api.ARConst.EnvironmentVariable
import nl.knaw.huc.annorepo.api.ContainerMetadata
import nl.knaw.huc.annorepo.auth.ARContainerUserDAO
import nl.knaw.huc.annorepo.auth.AROAuthAuthenticator
import nl.knaw.huc.annorepo.auth.ARUserDAO
import nl.knaw.huc.annorepo.auth.User
import nl.knaw.huc.annorepo.cli.EnvCommand
import nl.knaw.huc.annorepo.config.AnnoRepoConfiguration
import nl.knaw.huc.annorepo.filters.JSONPrettyPrintFilter
import nl.knaw.huc.annorepo.health.MongoDbHealthCheck
import nl.knaw.huc.annorepo.health.ServerHealthCheck
import nl.knaw.huc.annorepo.jobs.ExpiredChoresCleanerJob
import nl.knaw.huc.annorepo.resources.*
import nl.knaw.huc.annorepo.resources.tools.ContainerAccessChecker
import nl.knaw.huc.annorepo.resources.tools.IndexManager
import nl.knaw.huc.annorepo.resources.tools.SearchManager
import nl.knaw.huc.annorepo.resources.tools.formatAsSize
import nl.knaw.huc.annorepo.service.LocalDateTimeSerializer
import nl.knaw.huc.annorepo.service.MongoDbUpdater
import nl.knaw.huc.annorepo.service.UriFactory
import nl.knaw.huc.annorepo.tasks.JVMInfoTask
import nl.knaw.huc.annorepo.tasks.RecalculateFieldCountTask
import nl.knaw.huc.annorepo.tasks.UpdateTask

class AnnoRepoApplication : Application<AnnoRepoConfiguration?>() {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun getName(): String = APP_NAME

    override fun initialize(bootstrap: Bootstrap<AnnoRepoConfiguration?>) {
        bootstrap.configurationSourceProvider = SubstitutingSourceProvider(
            bootstrap.configurationSourceProvider, EnvironmentVariableSubstitutor()
        )
        bootstrap.addBundle(getSwaggerBundle())
        bootstrap.addBundle(JdbiExceptionsBundle())
        bootstrap.addBundle(getJobsBundle())
        bootstrap.addCommand(EnvCommand())
    }

    private fun getSwaggerBundle(): SwaggerBundle<AnnoRepoConfiguration> =
        object : SwaggerBundle<AnnoRepoConfiguration>() {
            override fun getSwaggerBundleConfiguration(configuration: AnnoRepoConfiguration): SwaggerBundleConfiguration =
                configuration.swaggerBundleConfiguration
        }

    private fun getJobsBundle(): ConfiguredBundle<in AnnoRepoConfiguration?> {
        val expiredChoresCleanerJob = ExpiredChoresCleanerJob()
        return JobsBundle(expiredChoresCleanerJob)
    }

    override fun run(configuration: AnnoRepoConfiguration?, environment: Environment) {
        log.info(
            "AR_ environment variables:\n\n" +
                    EnvironmentVariable.values()
                        .joinToString("\n") { e ->
                            "  ${e.name}:\t${System.getenv(e.name) ?: "(not set, using default)"}"
                        } +
                    "\n"
        )

        log.info("connecting to mongodb at ${configuration!!.mongodbURL} ...")
        val mongoClient = createMongoClient(configuration)
        log.info("connected!")

        val appVersion = javaClass.getPackage().implementationVersion
        val userDAO = ARUserDAO(configuration, mongoClient)
        val containerUserDAO = ARContainerUserDAO(configuration, mongoClient)
        val containerAccessChecker = ContainerAccessChecker(containerUserDAO)
        val searchManager = SearchManager(client = mongoClient, configuration = configuration)
        val indexManager = IndexManager(mongoClient.getDatabase(configuration.databaseName))
        val uriFactory = UriFactory(configuration)
        environment.jersey().apply {
            register(AboutResource(configuration, name, appVersion))
            register(HomePageResource())
            register(W3CResource(configuration, mongoClient, containerUserDAO, uriFactory))
            register(ContainerServiceResource(configuration, mongoClient, containerUserDAO, uriFactory, indexManager))
            register(
                GlobalServiceResource(
                    configuration,
                    mongoClient,
                    containerUserDAO,
                    searchManager,
                    uriFactory
                )
            )
            register(BatchResource(configuration, mongoClient, containerAccessChecker))
            if (configuration.prettyPrint) {
                register(JSONPrettyPrintFilter())
            }
            if (configuration.withAuthentication) {
                register(AdminResource(userDAO))
                register(MyResource(containerUserDAO))
                register(
                    AuthDynamicFeature(
                        OAuthCredentialAuthFilter.Builder<User>()
                            .setAuthenticator(AROAuthAuthenticator(userDAO))
                            .setPrefix("Bearer")
                            .buildAuthFilter()
                    )
                )
            }
//            register(ListResource(configuration, mongoClient, uriFactory))
        }
        environment.healthChecks().apply {
            register("server", ServerHealthCheck())
            register("mongodb", MongoDbHealthCheck(mongoClient))
        }

        environment.admin().apply {
            addTask(RecalculateFieldCountTask(mongoClient, configuration))
            addTask(UpdateTask(mongoClient, configuration))
            addTask(JVMInfoTask())
        }

        customizeObjectMapper(environment)

        doHealthChecks(environment)

        log.info(
            "\n\n  Starting $name (v$appVersion)\n" +
                    "    locally accessible at    " +
                    "http://localhost:${System.getenv(EnvironmentVariable.AR_SERVER_PORT.name) ?: 8080}\n" +
                    "    externally accessible at ${configuration.externalBaseUrl}\n"
        )
        val heapSpace = Runtime.getRuntime().totalMemory().formatAsSize
        log.info("Heap space = $heapSpace")

        MongoDbUpdater(
            configuration = configuration,
            client = mongoClient,
            userDAO = userDAO,
            containerUserDAO = containerUserDAO
        ).run()
    }

    private fun createMongoClient(configuration: AnnoRepoConfiguration): MongoClient {
        val mongoClient = KMongo.createClient(configuration.mongodbURL)
        val mdb = mongoClient.getDatabase(configuration.databaseName)
        val metadataCollectionExists = mdb.listCollectionNames()
            .firstOrNull { it == CONTAINER_METADATA_COLLECTION } == CONTAINER_METADATA_COLLECTION
        if (!metadataCollectionExists) {
            log.debug("creating container metadata collection + index")
            mdb.createCollection(CONTAINER_METADATA_COLLECTION)
            val containerMetadataCollection = mdb.getCollection<ContainerMetadata>(CONTAINER_METADATA_COLLECTION)
            containerMetadataCollection.createIndex(Indexes.ascending("name"))
        }
        return mongoClient
    }

    private val dateFormatString = "yyyy-MM-dd'T'HH:mm:ss"
    private fun customizeObjectMapper(environment: Environment) {
        val module = SimpleModule().apply {
            addSerializer(LocalDateTime::class.java, LocalDateTimeSerializer(dateFormatString))
        }
        environment.objectMapper.apply {
            dateFormat = SimpleDateFormat(dateFormatString)
            registerModule(module)
            registerKotlinModule()
        }
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