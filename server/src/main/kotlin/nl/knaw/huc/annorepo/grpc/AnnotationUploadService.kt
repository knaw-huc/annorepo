package nl.knaw.huc.annorepo.grpc

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import org.slf4j.LoggerFactory

class AnnotationUploadService : AnnotationUploadServiceGrpcKt.AnnotationUploadServiceCoroutineImplBase() {
    val log = LoggerFactory.getLogger(AnnotationUploadService::class.java)
    override fun addAnnotations(requests: Flow<AddAnnotationsRequest>): Flow<AddAnnotationsResponse> =
        requests
            .onEach { log.info("request={}", it) }
            .map {
                AddAnnotationsResponse
                    .newBuilder()
                    .addAllAnnotationIdentifier(
                        it.annotationList.map {
                            annotationIdentifier {
                                this.id = "x"
                                this.etag = "etag"
                            }
                        }
                    ).build()
            }
            .onEach { log.info("response={}", it) }
            .flowOn(context)
}
