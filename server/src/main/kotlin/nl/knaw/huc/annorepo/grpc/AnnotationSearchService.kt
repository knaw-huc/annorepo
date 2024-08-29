package nl.knaw.huc.annorepo.grpc

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.coroutines.flow.Flow

class AnnotationSearchService() : AnnotationSearchServiceGrpcKt.AnnotationSearchServiceCoroutineImplBase() {
    private val objectMapper = jacksonObjectMapper()

    override fun searchAnnotations(request: SearchAnnotationsRequest): Flow<SearchAnnotationsResponse> {
        TODO()
//        val headers = GrpcServerInterceptor.HEADERS_VALUE.get()
//        return flowOf()
    }

}
