package nl.knaw.huc.annorepo.grpc

import jakarta.ws.rs.NotFoundException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import nl.knaw.huc.annorepo.api.AnnotationIdentifier
import nl.knaw.huc.annorepo.api.WebAnnotationAsMap
import nl.knaw.huc.annorepo.dao.ContainerDAO

class AnnotationUploadService(
    private val containerDAO: ContainerDAO,
) : AnnotationUploadServiceGrpcKt.AnnotationUploadServiceCoroutineImplBase() {
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
        val containerName = "grpc-test-container"

        val annotationFlow: Flow<WebAnnotationAsMap> =
            requests.map {
                val json = it.annotationJson
                objectMapper.readValue<WebAnnotationAsMap>(json)
            }

        return storeAnnotations(containerName, annotationFlow)
            .onEach {
                log.info("identifier={}", it)
            }
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
        val annotations: MutableList<WebAnnotationAsMap> = mutableListOf()
        runBlocking { annotationFlow.collect { annotations.add(it) } }
        log.info("TODO: store ${annotations.size} annotations in $containerName")
        if (containerDAO.containerExists(containerName)){
            throw NotFoundException("Annotation Container '$containerName' not found")

        }
//        checkUserHasEditRightsInThisContainer(context, containerName)
        return containerDAO.addAnnotationsInBatch(containerName, annotations)
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
