package nl.knaw.huc.annorepo.resources

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson
import nl.knaw.huc.annorepo.api.AnnotationPage
import org.junit.jupiter.api.Test

internal class AnnotationPageTest {
    private val objectMapper = ObjectMapper().registerKotlinModule()

    @Test
    fun jsonSerializationIsAsExpected() {
        val ap = AnnotationPage(items = listOf(mapOf("key" to "value")), partOf = "http://example.org", startIndex = 0)
        val expectedJson = """
          {
            "type": "AnnotationPage",
            "as:items": {
              "@list": [{"key":"value"}]
            },
            "partOf": "http://example.org",
            "startIndex": 0
          }
        """.trimIndent()
        val json = objectMapper.writeValueAsString(ap)
        assertThatJson(json).isEqualTo(expectedJson)
    }
}