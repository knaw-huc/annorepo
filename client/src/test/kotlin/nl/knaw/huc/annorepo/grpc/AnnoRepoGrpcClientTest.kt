package nl.knaw.huc.annorepo.grpc

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import kotlinx.coroutines.runBlocking
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
}