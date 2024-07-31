package nl.knaw.huc.annorepo.resources

import org.junit.jupiter.api.Test
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson
import org.assertj.core.api.Assertions.assertThat
import nl.knaw.huc.annorepo.api.ContainerPage

internal class ContainerPageTest {

    private val objectMapper = jacksonObjectMapper()

    @Test
    fun `a ContainerPage without next serializes as expected`() {
        val ap = ContainerPage(
            id = "http://example.org/w3c/my-container/",
            label = "A Container for Web Annotations",
            annotations = listOf(),
            page = 0,
            total = 10,
        )
        val expectedJson = """
            {
              "@context": [
                "http://www.w3.org/ns/anno.jsonld",
                "http://www.w3.org/ns/ldp.jsonld"
              ],
              "id": "http://example.org/w3c/my-container/",
              "type": [
                "BasicContainer",
                "AnnotationCollection"
              ],
              "label": "A Container for Web Annotations",
              "first": {
                "id": "http://example.org/w3c/my-container/?page=0",
                "type": "AnnotationPage",
                "items":  [],
                "partOf": {
                    "id": "http://example.org/w3c/my-container/",
                    "type": "AnnotationCollection"
                },
                "startIndex": 0
              },
//              "last": "http://example.org/w3c/my-container/?page=1",
              "total": 10
            }
        """.trimIndent()
        val json = objectMapper.writeValueAsString(ap)
        assertThatJson(json).isEqualTo(expectedJson)
        assertThat(ap.label).isNotEmpty
    }

    @Test
    fun `a ContainerPage with next serializes as expected`() {
        val ap = ContainerPage(
            id = "http://example.org/w3c/my-container/",
            label = "A Container for Web Annotations",
            annotations = listOf(),
            page = 0,
            total = 100,
            prevPage = null,
            nextPage = 1
        )
        val expectedJson = """
            {
              "@context": [
                "http://www.w3.org/ns/anno.jsonld",
                "http://www.w3.org/ns/ldp.jsonld"
              ],
              "id": "http://example.org/w3c/my-container/",
              "type": [
                "BasicContainer",
                "AnnotationCollection"
              ],
              "label": "A Container for Web Annotations",
              "first": {
                "id": "http://example.org/w3c/my-container/?page=0",
                "type": "AnnotationPage",
                "items":  [],
                "partOf": {
                    "id": "http://example.org/w3c/my-container/",
                    "type": "AnnotationCollection"
                },
                "next": "http://example.org/w3c/my-container/?page=1",
                "startIndex": 0
              },
//              "last": "http://example.org/w3c/my-container/?page=1",
              "total": 100
            }
        """.trimIndent()
        val json = objectMapper.writeValueAsString(ap)
        assertThatJson(json).isEqualTo(expectedJson)
        assertThat(ap.label).isNotEmpty
    }

    @Test
    fun `a ContainerPage with both prev and next serializes as expected`() {
        val ap = ContainerPage(
            id = "http://example.org/w3c/my-container/",
            label = "A Container for Web Annotations",
            annotations = listOf(),
            page = 1,
            total = 100,
            prevPage = 0,
            nextPage = 2
        )
        val expectedJson = """
            {
              "@context": [
                "http://www.w3.org/ns/anno.jsonld",
                "http://www.w3.org/ns/ldp.jsonld"
              ],
              "id": "http://example.org/w3c/my-container/",
              "type": [
                "BasicContainer",
                "AnnotationCollection"
              ],
              "label": "A Container for Web Annotations",
              "first": {
                "id": "http://example.org/w3c/my-container/?page=1",
                "type": "AnnotationPage",
                "items":  [],
                "partOf": {
                    "id": "http://example.org/w3c/my-container/",
                    "type": "AnnotationCollection"
                },
                "prev": "http://example.org/w3c/my-container/?page=0",
                "next": "http://example.org/w3c/my-container/?page=2",
                "startIndex": 1
              },
//              "last": "http://example.org/w3c/my-container/?page=2",
              "total": 100
            }
        """.trimIndent()
        val json = objectMapper.writeValueAsString(ap)
        assertThatJson(json).isEqualTo(expectedJson)
        assertThat(ap.label).isNotEmpty
    }
}