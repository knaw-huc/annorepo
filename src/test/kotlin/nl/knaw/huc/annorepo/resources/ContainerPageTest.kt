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
    fun jsonSerializationIsAsExpected() {
        val ap = ContainerPage(
            id = "http://example.org/w3c/my-container/",
            label = "A Container for Web Annotations",
            annotations = listOf(),
            startIndex = 0,
            total = 0
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
              "last": "http://example.org/w3c/my-container/?page=0&desc=1",
              "total": 0
            }
        """.trimIndent()
        val json = objectMapper.writeValueAsString(ap)
        assertThatJson(json).isEqualTo(expectedJson)
        assertThat(ap.label).isNotEmpty
    }
}