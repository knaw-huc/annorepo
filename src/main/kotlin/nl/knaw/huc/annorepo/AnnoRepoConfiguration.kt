package nl.knaw.huc.annorepo

import com.fasterxml.jackson.annotation.JsonProperty
import io.dropwizard.Configuration
import io.dropwizard.db.DataSourceFactory
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration
import nl.knaw.huc.annorepo.resources.AboutResource
import org.slf4j.LoggerFactory

class AnnoRepoConfiguration : Configuration() {

    private val log = LoggerFactory.getLogger(javaClass)

    var baseUri: String
//    var dbServer: String
//    var dbPort: Int = 5432
//    var dbUser: String = "postgres"
//    var dbPassword: String

    private var database: DataSourceFactory = DataSourceFactory()

    @JsonProperty("database")
    fun setDataSourceFactory(factory: DataSourceFactory) {
        database = factory
    }

    @JsonProperty("database")
    fun getDataSourceFactory(): DataSourceFactory {
        return database
    }

    @JsonProperty("swagger")
    val swaggerBundleConfiguration = SwaggerBundleConfiguration()

    init {
        setDefaults()

        baseUri = getEnv("BASE_URI")
//        dbServer = getEnv("DB_SERVER")
//        dbPort = getEnv("DB_PORT").toInt()
//        dbUser = getEnv("DB_USER")
//        dbPassword = getEnv("DB_PASSWORD")
    }

    private fun setDefaults() {
        swaggerBundleConfiguration.resourcePackage = AboutResource::class.java.getPackage().name
    }

    private fun getEnv(name: String): String {
        val value = System.getenv(name)
        return if (value != null && value.isNotBlank()) {
            log.info("ENVIRONMENT VARIABLE $name = $value")
            value
        } else {
            log.info("ENVIRONMENT VARIABLE $name not set")
            ""
        }
    }

}