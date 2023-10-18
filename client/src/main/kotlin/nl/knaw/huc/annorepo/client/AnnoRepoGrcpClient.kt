package nl.knaw.huc.annorepo.client

import java.io.Closeable
import java.util.concurrent.TimeUnit
import io.grpc.ManagedChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import nl.knaw.huc.annorepo.grpc.AddAnnotationsResponse
import nl.knaw.huc.annorepo.grpc.AnnotationUploadServiceGrpcKt
import nl.knaw.huc.annorepo.grpc.HelloServiceGrpcKt
import nl.knaw.huc.annorepo.grpc.addAnnotationsRequest
import nl.knaw.huc.annorepo.grpc.sayHelloRequest

class AnnoRepoGrpcClient(private val channel: ManagedChannel) : Closeable {
    private val uploadStub: AnnotationUploadServiceGrpcKt.AnnotationUploadServiceCoroutineStub =
        AnnotationUploadServiceGrpcKt.AnnotationUploadServiceCoroutineStub(channel = channel)
    private val helloStub: HelloServiceGrpcKt.HelloServiceCoroutineStub =
        HelloServiceGrpcKt.HelloServiceCoroutineStub(channel = channel)

    suspend fun addContainerAnnotations(containerName: String, annotationsAsJson: Iterable<String>) {
        val request = addAnnotationsRequest {
            this.containerName = containerName
            this.annotation.addAll(annotationsAsJson)
        }
        val responseFlow: Flow<AddAnnotationsResponse> = uploadStub.addAnnotations(flowOf(request))
        println(responseFlow
//                .catch { print(it.message) }
            .map { """$containerName:${it.annotationIdentifierCount}""" }
            .toList())
    }

    suspend fun sayHello(name: String, apiKey: String) {
        val request = sayHelloRequest {
            this.name = name
            this.apiKey = apiKey
        }
        val response = helloStub.sayHello(request)
        println(response)
    }

    override fun close() {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS)
    }
}
