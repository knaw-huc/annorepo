package nl.knaw.huc.annorepo.resources

import org.junit.jupiter.api.Test
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson
import org.assertj.core.api.Assertions.assertThat
import nl.knaw.huc.annorepo.api.ANNO_JSONLD_URL
import nl.knaw.huc.annorepo.api.ContainerPage
import nl.knaw.huc.annorepo.api.LDP_JSONLD_URL
import nl.knaw.huc.annorepo.api.WebAnnotationAsMap

internal class ContainerPageTest {

    private val objectMapper = jacksonObjectMapper()

    @Test
    fun `container page context for page without annotations has the default context`() {
        val containerPage = ContainerPage(
            id = "id",
            label = "label",
            annotations = listOf(),
        )
        val expectedContext = listOf(ANNO_JSONLD_URL, LDP_JSONLD_URL)
        val context = containerPage.context
        assert(context == expectedContext)
    }

    @Test
    fun `container page context for page with annotations with custom context has the correct context aggregation`() {
        val annotationJson1 = """{
            |"@context":[
            |   "$ANNO_JSONLD_URL",
            |   "https://my-custom-contexts.nl/1.jsonld",
            |   {
            |       "ns": "https://example.com/namespace"
            |   }
            |],
            |"ns:custom": "custom",
            |"body":   "http://example.org/body1",
            |"target": "http://example.org/target1"
            |}""".trimMargin()
        val annotationJson2 = """{
            |"@context": [
            |   "$ANNO_JSONLD_URL",
            |   "https://my-custom-contexts.nl/2.jsonld",
            |   { "ex": "https://example.com/#" }
            |],
            |"body": { 
            |   "id": "http://example.org/body2",
            |   "ex:custom": "customvalue"
            |},
            |"target": { "id": "http://example.org/target2" }
            |}""".trimMargin()
        val annotationJson3 = """{
            |"@context": [
            |   "$ANNO_JSONLD_URL",
            |   "https://my-custom-contexts.nl/1.jsonld",
            |   { "ex": "https://example.com/#" }
            |],
            |"body": { 
            |   "id": "http://example.org/body3"
            |},
            |"target": { "id": "http://example.org/target3" }
            |}""".trimMargin()
        val containerPage = ContainerPage(
            id = "id",
            label = "label",
            annotations = listOf(annotationJson1, annotationJson2, annotationJson3)
                .map { objectMapper.readValue<WebAnnotationAsMap>(it) },
        )
        val expectedContext = listOf(
            ANNO_JSONLD_URL,
            LDP_JSONLD_URL,
            "https://my-custom-contexts.nl/1.jsonld",
            mapOf(
                "ns" to "https://example.com/namespace"
            ),
            "https://my-custom-contexts.nl/2.jsonld",
            mapOf(
                "ex" to "https://example.com/#",
            ),
        )
        val context = containerPage.context
        assertThat(context).isEqualTo(expectedContext)
//        println(objectMapper.writeValueAsString(containerPage))
    }

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