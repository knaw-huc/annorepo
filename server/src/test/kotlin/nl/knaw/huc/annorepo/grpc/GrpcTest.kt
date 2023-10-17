package nl.knaw.huc.annorepo.grpc

import java.io.Closeable
import java.util.concurrent.TimeUnit
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import nl.knaw.huc.annorepo.grpc.AnnotationUploadServiceGrpcKt.AnnotationUploadServiceCoroutineStub

class GrpcTest {

    class AnnoRepoGrpcClient(private val channel: ManagedChannel) : Closeable {
        private val stub: AnnotationUploadServiceCoroutineStub =
            AnnotationUploadServiceCoroutineStub(channel = channel)

        suspend fun addContainerAnnotations(containerName: String, annotationsAsJson: Iterable<String>) {
            val request = addAnnotationsRequest {
                this.containerName = containerName
                this.annotation.addAll(annotationsAsJson)
            }
//            val request = AddAnnotationsRequest.newBuilder().addAllAnnotation(annotationsAsJson).build()
            val responseFlow: Flow<AddAnnotationsResponse> = stub.addAnnotations(flowOf(request))
            println(responseFlow
//                .catch { print(it.message) }
                .map { """$containerName:${it.annotationIdentifierCount}""" }
                .toList())
        }

        override fun close() {
            channel.shutdown().awaitTermination(5, TimeUnit.SECONDS)
        }
    }

    @Disabled
    @Test
    fun `connect to grpc server`() {
        val annotations1 = listOf("{a}", "{b}")
        runBlocking {
            val channel: ManagedChannel = ManagedChannelBuilder.forAddress("localhost", 8000).usePlaintext().build()
            AnnoRepoGrpcClient(channel).use { client ->
                client.addContainerAnnotations("my-container", annotations1)
                client.addContainerAnnotations("my-container1", listOf("{C}", "{D}", "{E}", "{F}"))
            }
        }
    }
}