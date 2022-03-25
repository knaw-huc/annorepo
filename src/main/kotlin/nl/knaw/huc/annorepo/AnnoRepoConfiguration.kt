package nl.knaw.huc.annorepo

import com.fasterxml.jackson.annotation.JsonProperty
import io.dropwizard.Configuration
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration
import nl.knaw.huc.annorepo.resources.AboutResource

class AnnoRepoConfiguration() : Configuration() {

    @JsonProperty("swagger")
    val swaggerBundleConfiguration = SwaggerBundleConfiguration()

    init {
        setDefaults()
    }

    private fun setDefaults() {
        swaggerBundleConfiguration.resourcePackage = AboutResource::class.java.getPackage().name
    }

    fun getBaseURI(): String = ""
}