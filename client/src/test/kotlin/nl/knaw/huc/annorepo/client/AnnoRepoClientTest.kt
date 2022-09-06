package nl.knaw.huc.annorepo.client

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.net.URI
import kotlin.random.Random

const val ANNOREPO_BASE_URL = "http://localhost:9999"

class AnnoRepoClientTest {

    private val log = LoggerFactory.getLogger(javaClass)
    private val client = AnnoRepoClient(URI.create(ANNOREPO_BASE_URL), javaClass.canonicalName)

    @Test
    fun `client should connect, and GET about should return a map with at least a version field`() {
        assertThat(client).isNotNull
        val aboutInfo: Map<String, Any> = client.getAbout()
        assertThat(aboutInfo).containsKey("version")
        log.info("{}", aboutInfo)
    }

    @Test
    fun `createContainer without a preferred name should create a container with a generated name`() {
        val response = client.createContainer()
        assertThat(response.created).isTrue
        assertThat(response.location).startsWith(ANNOREPO_BASE_URL)
        assertThat(response.containerId).isNotNull
        assertThat(response.eTag).isNotNull
        client.deleteContainer(response.containerId, response.eTag)
    }

    @Test
    fun `createContainer with a preferred name should create a container with the preferred name`() {
        val preferredName = "container-name" + Random.nextInt()
        val response = client.createContainer(preferredName)
        assertThat(response.created).isTrue
        assertThat(response.location).endsWith("/$preferredName/")
        assertThat(response.containerId).isEqualTo(preferredName)
        assertThat(response.eTag).isNotNull
        val ok = client.deleteContainer(preferredName, response.eTag)
        assertThat(ok).isTrue
    }

}