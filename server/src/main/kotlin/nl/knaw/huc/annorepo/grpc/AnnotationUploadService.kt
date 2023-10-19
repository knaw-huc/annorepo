package nl.knaw.huc.annorepo.grpc

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import org.slf4j.LoggerFactory
import nl.knaw.huc.annorepo.api.WebAnnotationAsMap

class AnnotationUploadService : AnnotationUploadServiceGrpcKt.AnnotationUploadServiceCoroutineImplBase() {
    val log = LoggerFactory.getLogger(AnnotationUploadService::class.java)
    private val objectMapper = ObjectMapper().registerKotlinModule()

    override fun addAnnotations(requests: Flow<AddAnnotationsRequest>): Flow<AddAnnotationsResponse> =
        requests
            .onEach { log.info("request={}", it) }
            .onEach { processRequest(it) }
            .map {
                AddAnnotationsResponse
                    .newBuilder()
                    .addAllAnnotationIdentifier(
                        List(it.annotationList.size) { i ->
                            annotationIdentifier {
                                this.id = "x$i"
                                this.etag = "etag$i"
                            }
                        }
                    ).build()
            }
            .onEach { log.info("response={}", it) }
            .flowOn(context)

    private fun processRequest(request: AddAnnotationsRequest) {
        val containerName = request.containerName
        val annotationJsonList = request.annotationList
        val annotationList =
            annotationJsonList.map { objectMapper.readValue<WebAnnotationAsMap>(it) }.toList()
        log.info("container:{}", containerName)
        log.info("annotations:{}", annotationList)
    }
}
