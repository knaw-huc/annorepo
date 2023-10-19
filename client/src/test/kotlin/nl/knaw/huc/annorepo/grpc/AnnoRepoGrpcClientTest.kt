package nl.knaw.huc.annorepo.grpc

import java.net.URI
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import nl.knaw.huc.annorepo.api.WebAnnotation
import nl.knaw.huc.annorepo.client.AnnoRepoClient

class AnnoRepoGrpcClientTest {

    @Disabled
    @Test
    fun `connect to grpc server via annorepo client`() = runBlocking {
        val arc = AnnoRepoClient(serverURI = URI("http://localhost:8080"), apiKey = "something-or-other")
        val annotations1 = listOf(
            webAnnotation("annotation-1")
        )
        arc.usingGrpc { client ->
            val identifiers: List<AnnotationIdentifier> = client.addContainerAnnotations("my-container", annotations1)
            assertThat(identifiers).hasSize(1)
            val identifiers2 = client.addContainerAnnotations(
                "my-container1",
                listOf(
                    webAnnotation("annotation-2"),
                    webAnnotation("annotation-3")
                )
            )
            assertThat(identifiers2).hasSize(2)
            client.sayHello(
                "GRPC", "xxxxxxxxx"
            )
        }
        arc.usingGrpc { client ->
            client.sayHello("WORLD", "aaaaa")
        }
    }

    private fun webAnnotation(id: String) = WebAnnotation.Builder()
        .withBody(
            mapOf("id" to id)
        )
        .withTarget(
            mapOf("id" to "target")
        )
        .build()
        .asMap()
}