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

    override fun addAnnotations(requests: Flow<AddAnnotationsRequest>): Flow<AddAnnotationsResponse> {
        val headers = GrpcServerInterceptor.HEADERS_VALUE.get()
        val containerName = headers[GRPC_METADATA_KEY_CONTAINER_NAME] ?: "unknown"

        val annotationFlow: Flow<WebAnnotationAsMap> =
            requests
                .map {
                    val json = it.annotationJson
                    objectMapper.readValue<WebAnnotationAsMap>(json)
                }

        return storeAnnotations(containerName, annotationFlow)
            .map { identifier ->
                addAnnotationsResponse {
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
    ): List<AnnotationIdentifier> =
        runBlocking {
            val annotations: MutableList<WebAnnotationAsMap> = mutableListOf()
            annotationFlow.collect(annotations::add)
            if (!containerDAO.containerExists(containerName)) {
                throw NotFoundException("Annotation Container '$containerName' not found")
            }
            containerDAO.addAnnotationsInBatch(containerName, annotations)
        }
}
