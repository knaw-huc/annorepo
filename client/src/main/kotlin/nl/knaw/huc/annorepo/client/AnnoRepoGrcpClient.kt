package nl.knaw.huc.annorepo.client

import java.io.Closeable
import java.util.concurrent.TimeUnit
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.grpc.ManagedChannel
import io.grpc.Metadata
import io.grpc.stub.MetadataUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import nl.knaw.huc.annorepo.api.GRPC_METADATA_KEY_API_KEY
import nl.knaw.huc.annorepo.api.GRPC_METADATA_KEY_CONTAINER_NAME
import nl.knaw.huc.annorepo.api.WebAnnotationAsMap
import nl.knaw.huc.annorepo.grpc.AnnotationIdentifier
import nl.knaw.huc.annorepo.grpc.AnnotationUploadServiceGrpcKt.AnnotationUploadServiceCoroutineStub
import nl.knaw.huc.annorepo.grpc.HelloServiceGrpcKt.HelloServiceCoroutineStub
import nl.knaw.huc.annorepo.grpc.addAnnotationsRequest
import nl.knaw.huc.annorepo.grpc.sayHelloRequest

class AnnoRepoGrpcClient(private val channel: ManagedChannel, private val apiKey: String) : Closeable {
    private val metadata = Metadata().apply {
        setAsciiKey(GRPC_METADATA_KEY_API_KEY, apiKey)
    }

    private val uploadStub: AnnotationUploadServiceCoroutineStub =
        AnnotationUploadServiceCoroutineStub(channel = channel)
    private val helloStub: HelloServiceCoroutineStub =
        HelloServiceCoroutineStub(channel = channel)
    private val objectMapper = jacksonObjectMapper()

    fun addContainerAnnotations(
        containerName: String,
        annotations: List<WebAnnotationAsMap>
    ): Flow<AnnotationIdentifier> {
        val requests = annotations.map {
            addAnnotationsRequest {
                this.annotationJson = objectMapper.writeValueAsString(it)
            }
        }.asFlow()
        metadata.setAsciiKey(GRPC_METADATA_KEY_CONTAINER_NAME, containerName)
        return uploadStub
            .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata))
            .addAnnotations(requests)
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
