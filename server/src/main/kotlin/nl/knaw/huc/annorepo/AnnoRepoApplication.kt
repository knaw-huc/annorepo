package nl.knaw.huc.annorepo

import java.security.Principal
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.util.Collections.max
import java.util.concurrent.atomic.AtomicBoolean
import com.codahale.metrics.health.HealthCheck
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.mongodb.client.MongoClient
import com.mongodb.client.model.Indexes
import io.dropwizard.auth.AuthDynamicFeature
import io.dropwizard.auth.CachingAuthenticator
import io.dropwizard.auth.chained.ChainedAuthFilter
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
import org.apache.logging.log4j.kotlin.logger
import org.litote.kmongo.KMongo
import org.litote.kmongo.getCollection
import nl.knaw.huc.annorepo.api.ARConst.APP_NAME
import nl.knaw.huc.annorepo.api.ARConst.CONTAINER_METADATA_COLLECTION
import nl.knaw.huc.annorepo.api.ARConst.EnvironmentVariable
import nl.knaw.huc.annorepo.api.ContainerMetadata
import nl.knaw.huc.annorepo.auth.AROAuthAuthenticator
import nl.knaw.huc.annorepo.auth.OpenIDClient
import nl.knaw.huc.annorepo.auth.SRAMClient
import nl.knaw.huc.annorepo.auth.UnauthenticatedAuthFilter
import nl.knaw.huc.annorepo.auth.User
import nl.knaw.huc.annorepo.cli.EnvCommand
import nl.knaw.huc.annorepo.config.AnnoRepoConfiguration
import nl.knaw.huc.annorepo.dao.ARContainerDAO
import nl.knaw.huc.annorepo.dao.ARContainerUserDAO
import nl.knaw.huc.annorepo.dao.ARCustomQueryDAO
import nl.knaw.huc.annorepo.dao.ARUserDAO
import nl.knaw.huc.annorepo.filters.CorsFilter
import nl.knaw.huc.annorepo.filters.JSONPrettyPrintFilter
import nl.knaw.huc.annorepo.grpc.AnnotationUploadService
import nl.knaw.huc.annorepo.grpc.GrpcServerInterceptor
import nl.knaw.huc.annorepo.grpc.SayHelloService
import nl.knaw.huc.annorepo.health.MongoDbHealthCheck
import nl.knaw.huc.annorepo.health.ServerHealthCheck
import nl.knaw.huc.annorepo.jobs.ExpiredChoresCleanerJob
import nl.knaw.huc.annorepo.resources.AboutResource
import nl.knaw.huc.annorepo.resources.AdminResource
import nl.knaw.huc.annorepo.resources.BatchResource
import nl.knaw.huc.annorepo.resources.ContainerServiceResource
import nl.knaw.huc.annorepo.resources.GlobalServiceResource
import nl.knaw.huc.annorepo.resources.HomePageResource
import nl.knaw.huc.annorepo.resources.MyResource
import nl.knaw.huc.annorepo.resources.W3CResource
import nl.knaw.huc.annorepo.resources.tools.ContainerAccessChecker
import nl.knaw.huc.annorepo.resources.tools.FlagParamConverterProvider
import nl.knaw.huc.annorepo.resources.tools.IndexManager
import nl.knaw.huc.annorepo.resources.tools.SearchManager
import nl.knaw.huc.annorepo.resources.tools.formatAsSize
import nl.knaw.huc.annorepo.resources.tools.getMongoVersion
import nl.knaw.huc.annorepo.service.LocalDateTimeSerializer
import nl.knaw.huc.annorepo.service.MongoDbUpdater
import nl.knaw.huc.annorepo.service.UriFactory
import nl.knaw.huc.annorepo.tasks.JVMInfoTask
import nl.knaw.huc.annorepo.tasks.RecalculateFieldCountTask
import nl.knaw.huc.annorepo.tasks.UpdateTask

@OptIn(ExperimentalStdlibApi::class)
class AnnoRepoApplication : Application<AnnoRepoConfiguration?>() {

    override fun getName(): String = APP_NAME

    override fun initialize(bootstrap: Bootstrap<AnnoRepoConfiguration?>) {
        bootstrap.configurationSourceProvider = SubstitutingSourceProvider(
            bootstrap.configurationSourceProvider, EnvironmentVariableSubstitutor()
        )
//        bootstrap.addBundle(GrpcBridgeBundle())
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
        return JobsBundle(listOf(expiredChoresCleanerJob))
    }

    @OptIn(ExperimentalStdlibApi::class)
    override fun run(configuration: AnnoRepoConfiguration?, environment: Environment) {
        val maxEnvVarLen = max(EnvironmentVariable.entries.map { it.name.length })
        logger.info {
            "AR_ environment variables:\n\n" +
                    EnvironmentVariable.entries
                        .joinToString("\n") { e ->
                            "  ${e.name.padEnd(maxEnvVarLen + 1)}: ${System.getenv(e.name) ?: "(not set, using default)"}"
                        } +
                    "\n"
        }

        logger.info { "connecting to mongodb at ${configuration!!.mongodbURL} ..." }
        val mongoClient = createMongoClient(configuration!!)
        val mongoVersion = mongoClient.getMongoVersion()
        logger.info { "connected! version = $mongoVersion" }

        val appVersion = javaClass.getPackage().implementationVersion
        val uriFactory = UriFactory(configuration)

        val userDAO = ARUserDAO(configuration, mongoClient)
        val containerDAO = ARContainerDAO(configuration, mongoClient, uriFactory)
        val containerUserDAO = ARContainerUserDAO(configuration, mongoClient)
        val customQueryDAO = ARCustomQueryDAO(configuration, mongoClient)

        val containerAccessChecker = ContainerAccessChecker(containerUserDAO)
        val searchManager = SearchManager(containerDAO = containerDAO, configuration = configuration)
        val indexManager = IndexManager(containerDAO)

        configuration.grpc
            .builder(environment)
            .addService(AnnotationUploadService(containerDAO).bindService())
            .addService(SayHelloService().bindService())
            .intercept(GrpcServerInterceptor(userDAO, containerUserDAO))
            .build()

        val mongoVersionProducer = { mongoClient.getMongoVersion() }
        environment.jersey().apply {
            register(CorsFilter())
            register(AboutResource(configuration, name, appVersion, mongoVersionProducer))
            register(HomePageResource())
            register(W3CResource(configuration, containerDAO, containerUserDAO, uriFactory, indexManager))
            register(
                ContainerServiceResource(
                    configuration = configuration,
                    containerUserDAO = containerUserDAO,
                    containerDAO = containerDAO,
                    customQueryDAO = customQueryDAO,
                    uriFactory = uriFactory,
                    indexManager = indexManager,
                )
            )
            register(
                GlobalServiceResource(
                    configuration = configuration,
                    containerDAO = containerDAO,
                    containerUserDAO = containerUserDAO,
                    customQueryDAO = customQueryDAO,
                    searchManager = searchManager,
                    uriFactory = uriFactory
                )
            )
            register(BatchResource(configuration, containerDAO, containerAccessChecker))
            register(MyResource(containerDAO, containerUserDAO, uriFactory))
            register(FlagParamConverterProvider())
            if (configuration.prettyPrint) {
                register(JSONPrettyPrintFilter())
            }
            if (configuration.withAuthentication) {
                register(AdminResource(userDAO))
                val sramClient =
                    if (configuration.useSram()) {
                        SRAMClient(configuration.sram!!.applicationToken, configuration.sram!!.introspectUrl)
                    } else {
                        null
                    }
                val openIDClient = if (configuration.useOpenID()) {
                    OpenIDClient(configuration.openIDConfigurationUrl!!)
                } else {
                    null
                }
                val cachingAuthenticator = CachingAuthenticator(
                    environment.metrics(),
                    AROAuthAuthenticator(userDAO, sramClient, openIDClient),
                    configuration.authenticationCachePolicy
                )

                val oauthFilter = OAuthCredentialAuthFilter.Builder<User>()
                    .setAuthenticator(cachingAuthenticator)
                    .setPrefix("Bearer")
                    .buildAuthFilter()
                val anonymousFilter = UnauthenticatedAuthFilter<User>()
                register(
                    AuthDynamicFeature(
                        ChainedAuthFilter<User, Principal>(
                            listOf(oauthFilter, anonymousFilter)
                        )
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
            addTask(RecalculateFieldCountTask(containerDAO))
            addTask(UpdateTask(mongoClient, configuration))
            addTask(JVMInfoTask())
        }

        customizeObjectMapper(environment)

        doHealthChecks(environment)

        logger.info {
            "\n\n  Starting $name (v$appVersion)\n" +
                    "    locally accessible at    " +
                    "http://localhost:${System.getenv(EnvironmentVariable.AR_SERVER_PORT.name) ?: 8080}\n" +
                    "    externally accessible at ${configuration.externalBaseUrl}\n"
        }
        val heapSpace = Runtime.getRuntime().totalMemory().formatAsSize
        logger.info { "Heap space = $heapSpace" }

        MongoDbUpdater(
            configuration = configuration,
            client = mongoClient,
            userDAO = userDAO,
            containerUserDAO = containerUserDAO,
            containerDAO = containerDAO,
            indexManager = indexManager
        ).run()
    }

    private fun createMongoClient(configuration: AnnoRepoConfiguration): MongoClient {
        val mongoClient = KMongo.createClient(configuration.mongodbURL)
        val mdb = mongoClient.getDatabase(configuration.databaseName)
        val metadataCollectionExists = mdb.listCollectionNames()
            .firstOrNull { it == CONTAINER_METADATA_COLLECTION } == CONTAINER_METADATA_COLLECTION
        if (!metadataCollectionExists) {
            logger.debug { "creating container metadata collection + index" }
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
        logger.info { "Health checks:" }
        results.forEach { (name: String?, result: HealthCheck.Result) ->
            logger.info {
                "  $name: ${if (result.isHealthy) "healthy" else "unhealthy"}, message='${
                    StringUtils.defaultIfBlank(
                        result.message,
                        ""
                    )
                }'"
            }
            healthy.set(healthy.get() && result.isHealthy)
        }
        if (!healthy.get()) {
            throw RuntimeException("Failing health check(s)")
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            AnnoRepoApplication().run(*args)
        }
    }
}