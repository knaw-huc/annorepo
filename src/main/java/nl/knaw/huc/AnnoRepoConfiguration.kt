package nl.knaw.huc

import com.fasterxml.jackson.annotation.JsonProperty
import io.dropwizard.Configuration
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration
import nl.knaw.huc.resources.AboutResource

class AnnoRepoConfiguration internal constructor() : Configuration() {
    @JsonProperty("swagger")
    val swaggerBundleConfiguration = SwaggerBundleConfiguration()

    init {
        setDefaults()
    }

    private fun setDefaults() {
        val name = AboutResource::class.java.getPackage().name
        swaggerBundleConfiguration.resourcePackage = name
    }
}