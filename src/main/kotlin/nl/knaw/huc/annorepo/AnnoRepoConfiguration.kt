package nl.knaw.huc.annorepo

import com.fasterxml.jackson.annotation.JsonProperty
import io.dropwizard.Configuration
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration
import nl.knaw.huc.annorepo.resources.AboutResource
import org.slf4j.LoggerFactory

class AnnoRepoConfiguration : Configuration() {

    private val log = LoggerFactory.getLogger(javaClass)

    var baseUri: String
    var jdbcUri: String

    @JsonProperty("swagger")
    val swaggerBundleConfiguration = SwaggerBundleConfiguration()

    init {
        setDefaults()

        baseUri = getEnv("BASE_URI")
        jdbcUri = getEnv("JDBC_URI")
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