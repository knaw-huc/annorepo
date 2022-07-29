package nl.knaw.huc.annorepo.client

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.net.URI

class AnnoRepoClientTest {

    private val log = LoggerFactory.getLogger(javaClass)
    private val client = AnnoRepoClient(URI.create("http://localhost:9999"))

    @Test
    fun `client should connect, and GET about should return a map with at least a version field`() {
        assertThat(client).isNotNull
        val aboutInfo: Map<String, Any> = client.getAbout()
        assertThat(aboutInfo).containsKey("version")
        log.info("{}", aboutInfo)
    }
}