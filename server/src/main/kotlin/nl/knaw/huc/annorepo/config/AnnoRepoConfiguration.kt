package nl.knaw.huc.annorepo.config

import jakarta.validation.Valid
import com.fasterxml.jackson.annotation.JsonProperty
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
    @JsonProperty
    var authentication: AuthenticationConfiguration? = null

    val withAuthentication: Boolean
        get() = authentication != null

}