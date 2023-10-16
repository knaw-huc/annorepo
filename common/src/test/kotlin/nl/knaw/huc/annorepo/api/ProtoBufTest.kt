package nl.knaw.huc.annorepo.api

import org.junit.jupiter.api.Test
import org.assertj.core.api.Assertions.assertThat
import org.slf4j.LoggerFactory
import nl.knaw.huc.annorepo.grpc.AddAnnotationsRequest
import nl.knaw.huc.annorepo.grpc.AddAnnotationsResponse
import nl.knaw.huc.annorepo.grpc.AnnotationIdentifier

internal class ProtoBufTest {
    private val log = LoggerFactory.getLogger(javaClass)

    @Test
    fun `test AnnotationUploadService`() {
        val addAnnotationsRequest =
            AddAnnotationsRequest.newBuilder().setContainerName("my-container").addAllAnnotations(listOf("")).build()
        assertThat(addAnnotationsRequest.annotationsCount).isEqualTo(1)
        val ai = AnnotationIdentifier.newBuilder().setId("").setEtag("").build()
        val addAnnotationsResponse = AddAnnotationsResponse.newBuilder().addAnnotationIdentifiers(ai)
        assertThat(addAnnotationsResponse.annotationIdentifiersCount).isEqualTo(1)
    }
}
