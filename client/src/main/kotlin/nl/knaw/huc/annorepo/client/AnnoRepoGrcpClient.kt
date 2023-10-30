package nl.knaw.huc.annorepo.client

import java.io.Closeable
import java.util.concurrent.TimeUnit
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.grpc.ManagedChannel
import io.grpc.Metadata
import io.grpc.stub.MetadataUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import nl.knaw.huc.annorepo.api.WebAnnotationAsMap
import nl.knaw.huc.annorepo.grpc.AddAnnotationsResponse
import nl.knaw.huc.annorepo.grpc.AnnotationIdentifier
import nl.knaw.huc.annorepo.grpc.AnnotationUploadServiceGrpcKt.AnnotationUploadServiceCoroutineStub
import nl.knaw.huc.annorepo.grpc.HelloServiceGrpcKt.HelloServiceCoroutineStub
import nl.knaw.huc.annorepo.grpc.addAnnotationRequest
import nl.knaw.huc.annorepo.grpc.addAnnotationsRequest
import nl.knaw.huc.annorepo.grpc.sayHelloRequest

class AnnoRepoGrpcClient(private val channel: ManagedChannel, private val apiKey: String) : Closeable {
    private val metadata = Metadata().apply {
        setAsciiKey("api-key", apiKey)
    }

    private val uploadStub: AnnotationUploadServiceCoroutineStub =
        AnnotationUploadServiceCoroutineStub(channel = channel)
    private val helloStub: HelloServiceCoroutineStub =
        HelloServiceCoroutineStub(channel = channel)
    private val objectMapper = ObjectMapper().registerKotlinModule()

    suspend fun addContainerAnnotations(
        containerName: String,
        annotations: Iterable<WebAnnotationAsMap>
    ): List<AnnotationIdentifier> {
        val serializedAnnotations = annotations.map { objectMapper.writeValueAsString(it) }
        val request = addAnnotationsRequest {
            this.annotation.addAll(serializedAnnotations)
            this.apiKey = "not-used"
            this.containerName = "not-used"
        }
        metadata.setAsciiKey("container-name", containerName)
        val response: AddAnnotationsResponse = uploadStub
            .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata))
            .addAnnotations(request)
        return response.annotationIdentifierList
    }

    fun addContainerAnnotation(
        containerName: String,
        annotations: List<WebAnnotationAsMap>
    ): Flow<AnnotationIdentifier> {
        val requests = annotations.map {
            addAnnotationRequest {
                this.annotationJson = objectMapper.writeValueAsString(it)
                this.containerName = "not-used"
            }
        }.asFlow()
        metadata.setAsciiKey("container-name", containerName)
        return uploadStub
            .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata))
            .addAnnotation(requests)
            .map { it.annotationIdentifier }
    }

    suspend fun sayHello(name: String, apiKey: String) {
        val request = sayHelloRequest {
            this.name = name
            this.apiKey = apiKey
        }
        val response = helloStub
            .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata))
            .sayHello(request)
        println(response)
    }

    override fun close() {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS)
    }
}
