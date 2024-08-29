package nl.knaw.huc.annorepo.client

import java.net.URI
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.apache.logging.log4j.kotlin.logger
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail

const val ANNOREPO_BASE_URL = "http://localhost:2023"
//const val ANNOREPO_BASE_URL = "https://annorepo.globalise.huygens.knaw.nl"

@Disabled
class AnnoRepoClientTest {

    private val client = AnnoRepoClient(URI.create(ANNOREPO_BASE_URL), javaClass.canonicalName)

    @Test
    fun `client should connect, and GET about should return a map with at least a version field`() {
        assertThat(client).isNotNull
        client.getAbout().fold(
            { error -> fail<String>("Unexpected error: $error") },
            { result: ARResult.GetAboutResult ->
                assertThat(result.aboutInfo["version"]).isNotNull
                logger.info { result.aboutInfo }
            }
        )
        assertThat(client.serverNeedsAuthentication).isNotNull()
    }

//    @Test
//    fun `createContainer without a preferred name should create a container with a generated name`() {
//        client.createContainer().fold(
//            { error -> fail<String>("Unexpected error: $error") },
//            { response: ARResult.CreateContainerResult ->
//                assertThat(response.response.created).isTrue
//                assertThat(response.location).startsWith(ANNOREPO_BASE_URL)
//                assertThat(response.containerId).isNotNull
//                assertThat(response.eTag).isNotNull
//                client.deleteContainer(response.containerId, response.eTag)
//            }
//
//        )
//    }
//
//    //    @Test
//    fun `createContainer with a preferred name should create a container with the preferred name`() {
//        val preferredName = "container-name" + Random.nextInt()
//        client.createContainer(preferredName).fold(
//            { error -> fail<String>("Unexpected error: $error") },
//            { response ->
//                assertThat(response.created).isTrue
//                assertThat(response.location).endsWith("/$preferredName/")
//                assertThat(response.containerId).isEqualTo(preferredName)
//                assertThat(response.eTag).isNotNull
//                val ok = client.deleteContainer(preferredName, response.eTag).getOrElse { throw Exception() }
//                assertThat(ok).isTrue
//            }
//        )
//    }

}