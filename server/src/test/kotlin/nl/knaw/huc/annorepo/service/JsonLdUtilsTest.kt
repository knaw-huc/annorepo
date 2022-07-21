package nl.knaw.huc.annorepo.service

import nl.knaw.huc.annorepo.service.JsonLdUtils.checkFieldContext
import nl.knaw.huc.annorepo.service.JsonLdUtils.extractFields
import org.apache.jena.query.Dataset
import org.apache.jena.query.DatasetFactory
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.riot.JsonLDWriteContext
import org.apache.jena.riot.RDFFormat
import org.apache.jena.riot.RDFLanguages
import org.apache.jena.riot.RDFParser
import org.apache.jena.riot.RDFWriter
import org.apache.jena.riot.system.ErrorHandler
import org.apache.jena.sparql.core.DatasetGraph
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
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

    @Disabled
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

    @Disabled
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

    @Disabled
    @Test
    fun `a Web Annotation with custom fields with custom context is valid`() {
        val jsonld = """
            {
              "@context": [ "http://www.w3.org/ns/anno.jsonld",
                {
                  "extra": "http://example.org/customfields#extra",
                  "mycustomfield": "http://example.org/customfields#mycustomfield"
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

    @Disabled
    @Test
    fun `reading jsonld with jena`() {
        val jsonld = """
            {
              "@context": "http://www.w3.org/ns/anno.jsonld",
              "id": "urn:something",
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
        println("|$jsonld|")
        var dataset: Dataset
        // The parsers will do the necessary character set conversion.
        jsonld.byteInputStream().use { `in` ->
            val errorHandler = MyErrorHandler()
            val model = RDFParser
                .source(`in`)
                .lang(RDFLanguages.JSONLD)
                .errorHandler(errorHandler)
                .build()
                .toModel()
            println(model)
        }
        val model = ModelFactory.createDefaultModel()
        model.read(jsonld.byteInputStream(), "", "JSON-LD")
//        model.setNsPrefix("oa", "http://www.w3.org/ns/oa#")
        println()
        model.write(System.out, "TTL")
        println()
        val g: DatasetGraph = DatasetFactory.wrap(model).asDatasetGraph()
        val ctx = JsonLDWriteContext()
        ctx.setFrame("""{"@type":"http://www.w3.org/ns/oa#Annotation"}""")
        RDFWriter.create()
            .format(RDFFormat.JSONLD10_FRAME_PRETTY)
            .source(g)
            .context(ctx)
            .build()
            .output(System.out)
    }

    class MyErrorHandler : ErrorHandler {
        override fun warning(p0: String?, p1: Long, p2: Long) {
            TODO("Not yet implemented")
        }

        override fun error(p0: String?, p1: Long, p2: Long) {
            TODO("Not yet implemented")
        }

        override fun fatal(p0: String?, p1: Long, p2: Long) {
            TODO("Not yet implemented")
        }

    }
}