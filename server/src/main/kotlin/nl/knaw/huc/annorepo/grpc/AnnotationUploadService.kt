package nl.knaw.huc.annorepo.grpc

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import org.slf4j.LoggerFactory

class AnnotationUploadService : AnnotationUploadServiceGrpcKt.AnnotationUploadServiceCoroutineImplBase() {
    val log = LoggerFactory.getLogger(AnnotationUploadService::class.java)
    override fun addAnnotations(requests: Flow<AddAnnotationsRequest>): Flow<AddAnnotationsResponse> =
        requests
            .map { AddAnnotationsResponse.newBuilder().addAllAnnotationIdentifiers(listOf()).build() }
            .flowOn(context)
}
