package nl.knaw.huc.annorepo.grpc

import jakarta.ws.rs.NotFoundException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import nl.knaw.huc.annorepo.api.AnnotationIdentifier
import nl.knaw.huc.annorepo.api.GRPC_METADATA_KEY_CONTAINER_NAME
import nl.knaw.huc.annorepo.api.WebAnnotationAsMap
import nl.knaw.huc.annorepo.dao.ContainerDAO

class AnnotationUploadService(
    private val containerDAO: ContainerDAO,
) : AnnotationUploadServiceGrpcKt.AnnotationUploadServiceCoroutineImplBase() {
    val log: Logger = LoggerFactory.getLogger(AnnotationUploadService::class.java)
    private val objectMapper = ObjectMapper().registerKotlinModule()

    override suspend fun addAnnotations(request: AddAnnotationsRequest): AddAnnotationsResponse {
//        log.info("addAnnotations({})", request)
        processRequest(request)
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
//        log.info("addAnnotation({})", requests)
        val headers = GrpcServerInterceptor.HEADERS_VALUE.get()
        val containerName = headers[GRPC_METADATA_KEY_CONTAINER_NAME] ?: "unknown"

        val annotationFlow: Flow<WebAnnotationAsMap> =
            requests
//                .onEach { log.info("request = {}", it) }
                .map {
                    val json = it.annotationJson
                    objectMapper.readValue<WebAnnotationAsMap>(json)
                }

        return storeAnnotations(containerName, annotationFlow)
//            .onEach {
//                log.info("identifier={}", it)
//            }
            .map { identifier ->
                addAnnotationResponse {
                    annotationIdentifier = annotationIdentifier {
                        id = identifier.annotationName
                        etag = identifier.etag
                    }
                }
            }
            .asFlow()
    }

    private fun storeAnnotations(
        containerName: String,
        annotationFlow: Flow<WebAnnotationAsMap>
    ): List<AnnotationIdentifier> {
//        log.info("storeAnnotations")
        return runBlocking {
            val annotations: MutableList<WebAnnotationAsMap> = mutableListOf()
            annotationFlow.collect(annotations::add)
//            log.info("annotations.size={}", annotations.size)
            if (!containerDAO.containerExists(containerName)) {
                throw NotFoundException("Annotation Container '$containerName' not found")
            }
//        checkUserHasEditRightsInThisContainer(context, containerName)
            containerDAO.addAnnotationsInBatch(containerName, annotations)
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
