package nl.knaw.huc.annorepo.grpc

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class AnnotationSearchService() : AnnotationSearchServiceGrpcKt.AnnotationSearchServiceCoroutineImplBase() {
    val log: Logger = LoggerFactory.getLogger(AnnotationSearchService::class.java)
    private val objectMapper = ObjectMapper().registerKotlinModule()

    override fun searchAnnotations(request: SearchAnnotationsRequest): Flow<SearchAnnotationsResponse> {
        val headers = GrpcServerInterceptor.HEADERS_VALUE.get()

        return flowOf()
    }

}
