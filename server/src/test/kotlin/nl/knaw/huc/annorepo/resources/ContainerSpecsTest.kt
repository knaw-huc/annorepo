package nl.knaw.huc.annorepo.resources

import org.junit.jupiter.api.Test
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson
import nl.knaw.huc.annorepo.api.ANNO_JSONLD_URL
import nl.knaw.huc.annorepo.api.ContainerSpecs
import nl.knaw.huc.annorepo.api.LDP_JSONLD_URL

internal class ContainerSpecsTest {
    private val objectMapper = jacksonObjectMapper()

    @Test
    fun `ContainerSpecs json serialization works as expected`() {
        val spec = ContainerSpecs(
            context = listOf(
                ANNO_JSONLD_URL,
                LDP_JSONLD_URL
            ),
            type = listOf(
                "BasicContainer",
                "AnnotationCollection"
            ),
            label = "A Container for Web Annotations",
            readOnlyForAnonymousUsers = true
        )
        val json = objectMapper.writeValueAsString(spec)
        val expectedJson = """{
          "@context": [
            "http://www.w3.org/ns/anno.jsonld",
            "http://www.w3.org/ns/ldp.jsonld"
          ],
          "type": [
            "BasicContainer",
            "AnnotationCollection"
          ],
          "label": "A Container for Web Annotations",
          "readOnlyForAnonymousUsers": true
        }""".trimIndent()
        assertThatJson(json).isEqualTo(expectedJson)

        val spec2: ContainerSpecs = objectMapper.readValue(json)
        assertThatJson(spec2).isEqualTo(spec)
    }
}