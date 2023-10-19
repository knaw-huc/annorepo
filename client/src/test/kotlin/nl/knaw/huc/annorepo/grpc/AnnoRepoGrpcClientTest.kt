package nl.knaw.huc.annorepo.grpc

import java.net.URI
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import kotlinx.coroutines.runBlocking
import nl.knaw.huc.annorepo.client.AnnoRepoClient
import nl.knaw.huc.annorepo.client.AnnoRepoGrpcClient

class AnnoRepoGrpcClientTest {

    @Disabled
    @Test
    fun `connect to grpc server`() {
        val annotations1 = listOf("{a}", "{b}")
        runBlocking {
            val channel: ManagedChannel = ManagedChannelBuilder.forAddress("localhost", 8000).usePlaintext().build()
            AnnoRepoGrpcClient(channel).use { client ->
                client.addContainerAnnotations("my-container", annotations1)
                client.addContainerAnnotations("my-container1", listOf("{C}", "{D}", "{E}", "{F}"))
                client.sayHello("World", "xxxxxxxxx")
                client.sayHello("Who", "xxyyxxxxxxx")
            }
        }
    }

    @Disabled
    @Test
    fun `connect to grpc server via annorepo client`() = runBlocking {
        val arc = AnnoRepoClient(serverURI = URI("http://localhost:8080"))
        val annotations1 = listOf("{a}", "{b}")
        arc.usingGrpc { client ->
            client.addContainerAnnotations("my-container", annotations1)
            client.addContainerAnnotations("my-container1", listOf("{C}", "{D}", "{E}", "{F}"))
            client.sayHello("GRPC", "xxxxxxxxx")
            client.sayHello("CLIENT", "xxyyxxxxxxx")
        }
        arc.usingGrpc { client ->
            client.sayHello("WORLD", "aaaaa")
        }
    }
}