package nl.knaw.huc.annorepo.resources

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson
import nl.knaw.huc.annorepo.api.ANNO_JSONLD_URL
import nl.knaw.huc.annorepo.api.AnnotationPage
import org.junit.jupiter.api.Test

internal class AnnotationPageTest {
    private val objectMapper = ObjectMapper().registerKotlinModule()

    @Test
    fun `an AnnotationPage with minimal fields serializes as expected`() {
        val partOf = "http://example.org"
        val id = "http://example.org/container/?page=0"
        val startIndex = 0
        val ap = AnnotationPage(
            id = id,
            partOf = partOf,
            startIndex = startIndex,
            items = listOf(mapOf("key" to "value"))
        )
        val expectedJson = """
          {
            "id": "$id",
            "type": "AnnotationPage",
            "partOf": "$partOf",
            "startIndex": $startIndex,
            "items": [{"key":"value"}]
          }
        """.trimIndent()
        val json = objectMapper.writeValueAsString(ap)
        assertThatJson(json).isEqualTo(expectedJson)
    }

    @Test
    fun `an AnnotationPage with all fields serializes as expected`() {
        val id = "http://example.org/container/?page=1"
        val partOf = "http://example.org"
        val startIndex = 0
        val prev = "http://example.org/container/?page=0"
        val next = "http://example.org/container/?page=2"
        val ap = AnnotationPage(
            id = id,
            partOf = partOf,
            startIndex = startIndex,
            items = listOf(mapOf("key" to "value")),
            context = listOf(ANNO_JSONLD_URL),
            prev = prev,
            next = next
        )
        val expectedJson = """
          {
            "@context": ["$ANNO_JSONLD_URL"],
            "id": "$id",
            "type": "AnnotationPage",
            "partOf": "$partOf",
            "startIndex": $startIndex,
            "prev": "$prev",
            "next": "$next",
            "items": [{"key":"value"}]
          }
        """.trimIndent()
        val json = objectMapper.writeValueAsString(ap)
        assertThatJson(json).isEqualTo(expectedJson)
    }
}