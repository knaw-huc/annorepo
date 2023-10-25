package nl.knaw.huc.annorepo.client

import java.io.Closeable
import java.util.concurrent.TimeUnit
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.grpc.ManagedChannel
import io.grpc.Metadata
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import nl.knaw.huc.annorepo.api.WebAnnotationAsMap
import nl.knaw.huc.annorepo.grpc.AddAnnotationsResponse
import nl.knaw.huc.annorepo.grpc.AnnotationIdentifier
import nl.knaw.huc.annorepo.grpc.AnnotationUploadServiceGrpcKt
import nl.knaw.huc.annorepo.grpc.HelloServiceGrpcKt
import nl.knaw.huc.annorepo.grpc.addAnnotationRequest
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

    fun addContainerAnnotation(
        containerName: String,
        annotations: List<WebAnnotationAsMap>
    ): Flow<AnnotationIdentifier> {
        val requests = annotations.map {
            addAnnotationRequest {
                this.containerName = containerName
                this.annotationJson = objectMapper.writeValueAsString(it)
            }
        }.asFlow()
        val metadata = Metadata()
        metadata.put(Metadata.Key.of("api-key", Metadata.ASCII_STRING_MARSHALLER), "xxxx")
        return uploadStub.addAnnotation(requests, metadata).map { it.annotationIdentifier }
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
