package nl.knaw.huc.annorepo.resources

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson
import org.junit.jupiter.api.Test

internal class ContainerSpecsTest {
    @Test
    fun getLabel() {
        val spec = ContainerSpecs(
            context = listOf(
                "http://www.w3.org/ns/anno.jsonld",
                "http://www.w3.org/ns/ldp.jsonld"
            ),
            type = listOf(
                "BasicContainer",
                "AnnotationCollection"
            ),
            label = "A Container for Web Annotations"
        )
        val objectMapper = ObjectMapper().registerKotlinModule()
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
          "label": "A Container for Web Annotations"
        }""".trimIndent()
        assertThatJson(json).isEqualTo(expectedJson)

        val spec2 = objectMapper.readValue(json, ContainerSpecs::class.java)
        assertThatJson(spec2).isEqualTo(spec)
    }
}