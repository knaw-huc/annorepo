package nl.knaw.huc.annorepo.resources

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson
import nl.knaw.huc.annorepo.api.ContainerPage
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class ContainerPageTest {

    private val objectMapper = ObjectMapper().registerKotlinModule()

    @Test
    fun jsonSerializationWithoutNextPageIsAsExpected() {
        val ap = ContainerPage(
            id = "http://example.org/w3c/my-container/",
            label = "A Container for Web Annotations",
            annotations = listOf(),
            page = 0,
            total = 10,
            prevPage = null,
            nextPage = null,
            lastPage = 1
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
                "type": "AnnotationPage",
                "items":  [],
                "partOf": "http://example.org/w3c/my-container/",
                "startIndex": 0
              },
              "last": "http://example.org/w3c/my-container/?page=1",
              "total": 10
            }
        """.trimIndent()
        val json = objectMapper.writeValueAsString(ap)
        assertThatJson(json).isEqualTo(expectedJson)
        assertThat(ap.label).isNotEmpty
    }

    @Test
    fun jsonSerializationWithNextPageIsAsExpected() {
        val ap = ContainerPage(
            id = "http://example.org/w3c/my-container/",
            label = "A Container for Web Annotations",
            annotations = listOf(),
            page = 0,
            total = 100,
            prevPage = null,
            nextPage = 1,
            lastPage = 1
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
                "type": "AnnotationPage",
                "items":  [],
                "partOf": "http://example.org/w3c/my-container/",
                "startIndex": 0
              },
              "next": "http://example.org/w3c/my-container/?page=1",
              "last": "http://example.org/w3c/my-container/?page=1",
              "total": 100
            }
        """.trimIndent()
        val json = objectMapper.writeValueAsString(ap)
        assertThatJson(json).isEqualTo(expectedJson)
        assertThat(ap.label).isNotEmpty
    }

    @Test
    fun jsonSerializationWithPrevAndNextPageIsAsExpected() {
        val ap = ContainerPage(
            id = "http://example.org/w3c/my-container/",
            label = "A Container for Web Annotations",
            annotations = listOf(),
            page = 1,
            total = 100,
            prevPage = 0,
            nextPage = 2,
            lastPage = 2
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
                "type": "AnnotationPage",
                "items":  [],
                "partOf": "http://example.org/w3c/my-container/",
                "startIndex": 1
              },
              "prev": "http://example.org/w3c/my-container/?page=0",
              "next": "http://example.org/w3c/my-container/?page=2",
              "last": "http://example.org/w3c/my-container/?page=2",
              "total": 100
            }
        """.trimIndent()
        val json = objectMapper.writeValueAsString(ap)
        assertThatJson(json).isEqualTo(expectedJson)
        assertThat(ap.label).isNotEmpty
    }
}