package nl.knaw.huc.annorepo.client

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.net.URI

internal class AnnoRepoClientTest {

    @Test
    fun `client should connect`() {
        val client = AnnoRepoClient(URI.create("http://localhost:9000"))
        assertThat(client).isNotNull
    }
}