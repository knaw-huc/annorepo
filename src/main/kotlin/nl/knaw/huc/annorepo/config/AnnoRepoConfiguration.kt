package nl.knaw.huc.annorepo.config

import com.fasterxml.jackson.annotation.JsonProperty
import io.dropwizard.Configuration
import io.dropwizard.db.DataSourceFactory
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration
import nl.knaw.huc.annorepo.resources.AboutResource
import org.slf4j.LoggerFactory
import javax.validation.Valid
import javax.validation.constraints.NotNull

class AnnoRepoConfiguration : Configuration() {

    val pageSize: Int = 100

    private val log = LoggerFactory.getLogger(javaClass)

    @Valid
    @NotNull
    @JsonProperty
    var externalBaseUrl = ""

    @Valid
    @NotNull
    @JsonProperty
    val database = DataSourceFactory()

    @Valid
    @NotNull
    @JsonProperty("elasticsearch")
    val elasticSearchConfiguration = ElasticSearchConfiguration()

    @Valid
    @NotNull
    @JsonProperty("swagger")
    val swaggerBundleConfiguration = SwaggerBundleConfiguration()

    init {
        setDefaults()
    }

    private fun setDefaults() {
        swaggerBundleConfiguration.resourcePackage = AboutResource::class.java.getPackage().name
    }

}