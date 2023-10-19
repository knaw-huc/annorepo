package nl.knaw.huc.annorepo.client

import java.io.Closeable
import java.util.concurrent.TimeUnit
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.grpc.ManagedChannel
import nl.knaw.huc.annorepo.api.WebAnnotationAsMap
import nl.knaw.huc.annorepo.grpc.AddAnnotationsResponse
import nl.knaw.huc.annorepo.grpc.AnnotationIdentifier
import nl.knaw.huc.annorepo.grpc.AnnotationUploadServiceGrpcKt
import nl.knaw.huc.annorepo.grpc.HelloServiceGrpcKt
import nl.knaw.huc.annorepo.grpc.addAnnotationsRequest
import nl.knaw.huc.annorepo.grpc.sayHelloRequest

class AnnoRepoGrpcClient(private val channel: ManagedChannel, private val apiKey: String) : Closeable {
    private val uploadStub: AnnotationUploadServiceGrpcKt.AnnotationUploadServiceCoroutineStub =
        AnnotationUploadServiceGrpcKt.AnnotationUploadServiceCoroutineStub(channel = channel)
    private val helloStub: HelloServiceGrpcKt.HelloServiceCoroutineStub =
        HelloServiceGrpcKt.HelloServiceCoroutineStub(channel = channel)
    private val objectMapper = ObjectMapper().registerKotlinModule()

    suspend fun addContainerAnnotations(
        containerName: String,
        annotations: Iterable<WebAnnotationAsMap>
    ): List<AnnotationIdentifier> {
        val serializedAnnotations = annotations.map { objectMapper.writeValueAsString(it) }
        val request = addAnnotationsRequest {
            this.apiKey = this@AnnoRepoGrpcClient.apiKey
            this.containerName = containerName
            this.annotation.addAll(serializedAnnotations)
        }
        val response: AddAnnotationsResponse = uploadStub.addAnnotations(request)
        return response.annotationIdentifierList
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
