package nl.knaw.huc.annorepo.config

import jakarta.validation.Valid
import com.fasterxml.jackson.annotation.JsonProperty
import com.github.benmanes.caffeine.cache.CaffeineSpec
import io.dropwizard.jobs.JobConfiguration
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration
import org.jetbrains.annotations.NotNull
import nl.knaw.huc.annorepo.api.ARConst
import nl.knaw.huc.annorepo.resources.AboutResource

open class AnnoRepoConfiguration : JobConfiguration() {

    @Valid
    @NotNull
    @JsonProperty
    var prettyPrint: Boolean = true

    @Valid
    @NotNull
    @JsonProperty
    var mongodbURL: String = "mongodb://localhost/"

    @Valid
    @NotNull
    @JsonProperty
    var databaseName: String = "annorepo"

    @Valid
    @NotNull
    @JsonProperty
    var pageSize: Int = 100

    @Valid
    @NotNull
    @JsonProperty
    var externalBaseUrl = ""

    @Valid
    @NotNull
    @JsonProperty
    var rangeSelectorType = "urn:republic:TextAnchorSelector"

    @Valid
    @JsonProperty
    var withAuthentication: Boolean = false

    @Valid
    @JsonProperty
    var sram: SramConfiguration? = null

    @Valid
    @NotNull
    @JsonProperty
    var rootApiKey: String = "YouIntSeenMeRoit"

    @Valid
    @NotNull
    @JsonProperty("swagger")
    val swaggerBundleConfiguration = SwaggerBundleConfiguration().apply {
        resourcePackage = AboutResource::class.java.getPackage().name
        version = javaClass.getPackage().implementationVersion
        title = ARConst.APP_NAME
        license = "Apache 2.0"
        licenseUrl = "http://www.apache.org/licenses/"
        contactUrl = "https://github.com/knaw-huc/annorepo"
        contact = ARConst.APP_NAME
    }

    @Valid
    @NotNull
    @JsonProperty
    var grpc: GrpcFactory = GrpcFactory()

    @Valid
    @NotNull
    @JsonProperty
    var authenticationCachePolicy: CaffeineSpec? = null

    @Valid
    @JsonProperty
    var openIDConfigurationUrl: String? = null

    fun useSram() = sram?.applicationToken != null
            && sram?.applicationToken != "default-value"

    fun useOpenID() = openIDConfigurationUrl != null
            && openIDConfigurationUrl != "default-value"

}