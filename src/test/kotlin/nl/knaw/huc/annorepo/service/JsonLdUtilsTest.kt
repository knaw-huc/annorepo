package nl.knaw.huc.annorepo.service

import nl.knaw.huc.annorepo.service.JsonLdUtils.checkFieldContext
import nl.knaw.huc.annorepo.service.JsonLdUtils.extractFields
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class JsonLdUtilsTest {

    @Test
    fun `listFields() lists all fields in the given jsonld`() {
        val jsonld = """
            {
              "@context": "http://www.w3.org/ns/anno.jsonld",
              "type": "Annotation",
              "body": {
                "type": "TextualBody",
                "value": "I like this page!",
                "extra": {
                    "key1":  "value",
                    "key2":  "value"
                },
                "element_list": [1,2,3],
                "objectlist": [
                    {"key":"value"},
                    {"key":"value", "optional":"42"}
                ]
              },
              "target": "http://www.example.com/index.html"
            }
        """.trimMargin()
        val fields = extractFields(jsonld)
        assertThat(fields).containsExactlyInAnyOrder(
            "@context",
            "type",
            "body.type",
            "body.value",
            "body.extra.key1",
            "body.extra.key2",
            "body.element_list",
            "body.objectlist.key",
            "body.objectlist.optional",
            "target"
        )
    }

    @Test
    fun `a Web Annotation without custom fields is valid`() {
        val jsonld = """
            {
              "@context": "http://www.w3.org/ns/anno.jsonld",
              "type": "Annotation",
              "body": {
                "type": "TextualBody",
                "value": "I like this page!"
              },
              "target": "http://www.example.com/index.html"
            }
        """.trimMargin()
        val report = checkFieldContext(jsonld)
        assertThat(report.isValid).isTrue
        assertThat(report.invalidFields).isEmpty()
    }

    @Test
    fun `a Web Annotation with plain custom fields without context is invalid`() {
        val jsonld = """
            {
              "@context": "http://www.w3.org/ns/anno.jsonld",
              "type": "Annotation",
              "body": {
                "type": "TextualBody",
                "extra": "additional value",
                "value": "I like this page!"
              },
              "target": "http://www.example.com/index.html",
              "mycustomfield": "my custom value"
            }
        """.trimMargin()
        val report = checkFieldContext(jsonld)
        assertThat(report.isValid).isFalse
        assertThat(report.invalidFields).containsExactly("body.extra", "mycustomfield")
    }

    @Test
    fun `a Web Annotation with custom fields with custom context is valid`() {
        val jsonld = """
            {
              "@context": [ "http://www.w3.org/ns/anno.jsonld",
                {
                  "extra": "http://example.org/customfields#extra",
                  "mycustomfield": "http://example.org/customfields#mycustomfield",
                }
              ],
              "type": "Annotation",
              "body": {
                "type": "TextualBody",
                "extra": "additional value",
                "value": "I like this page!"
              },
              "target": "http://www.example.com/index.html",
              "mycustomfield": "my custom value"
            }
        """.trimMargin()
        val report = checkFieldContext(jsonld)
        assertThat(report.isValid).isTrue
        assertThat(report.invalidFields).isEmpty()
    }
}