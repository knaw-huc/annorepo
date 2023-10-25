package nl.knaw.huc.annorepo.grpc

import java.util.UUID
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.slf4j.LoggerFactory
import nl.knaw.huc.annorepo.api.WebAnnotationAsMap

class AnnotationUploadService : AnnotationUploadServiceGrpcKt.AnnotationUploadServiceCoroutineImplBase() {
    val log = LoggerFactory.getLogger(AnnotationUploadService::class.java)
    private val objectMapper = ObjectMapper().registerKotlinModule()

    override suspend fun addAnnotations(request: AddAnnotationsRequest): AddAnnotationsResponse {
        val processed = processRequest(request)
        return AddAnnotationsResponse
            .newBuilder()
            .addAllAnnotationIdentifier(
                List(request.annotationList.size) { i ->
                    annotationIdentifier {
                        this.id = "x$i"
                        this.etag = "etag$i"
                    }
                }
            ).build()
    }

    override fun addAnnotation(requests: Flow<AddAnnotationRequest>): Flow<AddAnnotationResponse> {
        return requests.map {
            addAnnotationResponse {
                annotationIdentifier = annotationIdentifier { id = UUID.randomUUID().toString(); etag = "my-etag" }
            }
        }
    }

    private fun processRequest(request: AddAnnotationsRequest) {
        val containerName = request.containerName
        val annotationJsonList = request.annotationList
        val annotationList =
            annotationJsonList.map { objectMapper.readValue<WebAnnotationAsMap>(it) }.toList()
        log.info("container:{}", containerName)
        log.info("annotations:{}", annotationList)
    }
}
