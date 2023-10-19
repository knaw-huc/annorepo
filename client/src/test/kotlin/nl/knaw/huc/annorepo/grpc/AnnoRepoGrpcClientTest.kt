package nl.knaw.huc.annorepo.grpc

import java.net.URI
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import kotlinx.coroutines.runBlocking
import nl.knaw.huc.annorepo.api.WebAnnotation
import nl.knaw.huc.annorepo.client.AnnoRepoClient

class AnnoRepoGrpcClientTest {

    @Disabled
    @Test
    fun `connect to grpc server via annorepo client`() = runBlocking {
        val arc = AnnoRepoClient(serverURI = URI("http://localhost:8080"))
        val annotations1 = listOf(
            webAnnotation("annotation-1")
        )
        arc.usingGrpc { client ->
            client.addContainerAnnotations("my-container", annotations1)
            client.addContainerAnnotations(
                "my-container1",
                listOf(
                    webAnnotation("annotation-2"),
                    webAnnotation("annotation-3")
                )
            )
            client.sayHello(
                "GRPC", "xxxxxxxxx"
            )
            client.sayHello("CLIENT", "xxyyxxxxxxx")
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