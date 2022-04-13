package nl.knaw.huc.annorepo

import com.fasterxml.jackson.annotation.JsonProperty
import io.dropwizard.Configuration
import io.dropwizard.db.DataSourceFactory
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration
import nl.knaw.huc.annorepo.resources.AboutResource
import org.slf4j.LoggerFactory

class AnnoRepoConfiguration : Configuration() {

    private val log = LoggerFactory.getLogger(javaClass)

    @JsonProperty
    var externalBaseUrl = ""

    @JsonProperty
    val database = DataSourceFactory()

//    @JsonProperty("database")
//    fun setDataSourceFactory(factory: DataSourceFactory) {
//        database = factory
//    }
//
//    @JsonProperty("database")
//    fun getDataSourceFactory(): DataSourceFactory {
//        return database
//    }

    @JsonProperty("swagger")
    val swaggerBundleConfiguration = SwaggerBundleConfiguration()

    init {
        setDefaults()
    }

    private fun setDefaults() {
        swaggerBundleConfiguration.resourcePackage = AboutResource::class.java.getPackage().name
    }

}