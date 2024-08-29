package nl.knaw.huc.annorepo.resources

import org.junit.jupiter.api.Test
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson
import nl.knaw.huc.annorepo.api.ANNO_JSONLD_URL
import nl.knaw.huc.annorepo.api.AnnotationPage
import nl.knaw.huc.annorepo.resources.tools.annotationCollectionLink

internal class AnnotationPageTest {
    private val objectMapper = jacksonObjectMapper()

    @Test
    fun `an AnnotationPage with minimal fields serializes as expected`() {
        val partOf = "http://example.org"
        val id = "http://example.org/container/?page=0"
        val startIndex = 0
        val ap = AnnotationPage(
            id = id,
            partOf = annotationCollectionLink(partOf),
            startIndex = startIndex,
            items = listOf(mapOf("key" to "value"))
        )
        val expectedJson = """
          {
            "id": "$id",
            "type": "AnnotationPage",
            "partOf": {"id":"$partOf","type":"AnnotationCollection"},
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
            partOf = annotationCollectionLink(partOf),
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
            "partOf": {"id":"$partOf","type":"AnnotationCollection"},
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