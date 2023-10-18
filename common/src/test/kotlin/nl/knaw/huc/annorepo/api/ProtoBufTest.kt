package nl.knaw.huc.annorepo.api

import org.junit.jupiter.api.Test
import org.assertj.core.api.Assertions.assertThat
import org.slf4j.LoggerFactory
import nl.knaw.huc.annorepo.grpc.AddAnnotationsRequest
import nl.knaw.huc.annorepo.grpc.AddAnnotationsResponse
import nl.knaw.huc.annorepo.grpc.AnnotationIdentifier
import nl.knaw.huc.annorepo.grpc.annotationIdentifier

internal class ProtoBufTest {
    private val log = LoggerFactory.getLogger(javaClass)

    @Test
    fun `test AnnotationUploadService`() {
        val addAnnotationsRequest =
            AddAnnotationsRequest.newBuilder().setContainerName("my-container").addAllAnnotation(listOf("")).build()
        assertThat(addAnnotationsRequest.annotationCount).isEqualTo(1)
        val ai = AnnotationIdentifier.newBuilder().setId("").setEtag("").build()
        val ai2 = annotationIdentifier {
            id = ""
            etag = ""
        }
        val addAnnotationsResponse = AddAnnotationsResponse.newBuilder().addAnnotationIdentifier(ai).build()
        assertThat(addAnnotationsResponse.annotationIdentifierCount).isEqualTo(1)
    }
}
