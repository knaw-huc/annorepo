import java.net.URI
import org.junit.jupiter.api.Test
import org.assertj.core.api.Assertions.assertThat
import nl.knaw.huc.annorepo.client.AnnoRepoClient

class AnnoRepoClientTest {
    @Test
    fun `calling create with a URI that does not resolve returns null`() {
        val client: AnnoRepoClient? = AnnoRepoClient.create(URI.create("http://no-annorepo-here"))
        assertThat(client).isNull()
    }

    @Test
    fun `calling create with a URI that does not represent an AnnoRepo server returns null`() {
        val client: AnnoRepoClient? = AnnoRepoClient.create(URI.create("https://example.com"))
        assertThat(client).isNull()
    }
}