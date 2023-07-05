package nl.knaw.huc.annorepo.resources

import jakarta.ws.rs.core.MediaType.APPLICATION_JSON
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import com.jayway.jsonpath.JsonPath
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport
import io.dropwizard.testing.junit5.ResourceExtension
import org.assertj.core.api.Assertions.assertThat
import nl.knaw.huc.annorepo.config.AnnoRepoConfiguration

private const val APP_NAME = "annorepo"
private const val VERSION = "0.1.0"
private const val MONGO_VERSION = "0.1.0"
private const val BASE_URI = "https://annorepo.com"

@ExtendWith(DropwizardExtensionsSupport::class)
open class AboutResourceTest {
    private val config: AnnoRepoConfiguration = AnnoRepoConfiguration().apply { externalBaseUrl = BASE_URI }
    private val resource = ResourceExtension
        .builder()
        .addResource(AboutResource(config, APP_NAME, VERSION, MONGO_VERSION))
        .build()
//    private val log = LoggerFactory.getLogger(javaClass)

    @Test
    fun `about endpoint returns expected json`() {
        val response = resource
            .client()
            .target("/about")
            .request(APPLICATION_JSON)
            .get()
        assertThat(response.status).isEqualTo(200)
        val body = response.readEntity(String::class.java)
        val json = JsonPath.parse(body)

        assertThat(json.read("$.appName", String::class.java)).isEqualTo(APP_NAME)
        assertThat(json.read("$.version", String::class.java)).isEqualTo(VERSION)
        assertThat(json.read("$.baseURI", String::class.java)).isEqualTo(BASE_URI)
    }
}