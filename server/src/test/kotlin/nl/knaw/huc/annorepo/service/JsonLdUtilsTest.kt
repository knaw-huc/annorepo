package nl.knaw.huc.annorepo.service

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.apache.jena.query.DatasetFactory
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.riot.JsonLDWriteContext
import org.apache.jena.riot.RDFFormat
import org.apache.jena.riot.RDFLanguages
import org.apache.jena.riot.RDFParser
import org.apache.jena.riot.RDFWriter
import org.apache.jena.riot.system.ErrorHandler
import org.apache.jena.sparql.core.DatasetGraph
import org.apache.logging.log4j.kotlin.logger
import org.assertj.core.api.Assertions.assertThat
import nl.knaw.huc.annorepo.service.JsonLdUtils.checkFieldContext
import nl.knaw.huc.annorepo.service.JsonLdUtils.extractFields

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
              "@context": ["http://www.w3.org/ns/anno.jsonld",{"ex":"http://example.org/"}],
              "id": "urn:something",
              "type": "Annotation",
              "body": {
                "type": "TextualBody",
                "ex:extra": "additional value",
                "value": "I like this page!"
              },
              "target": "http://www.example.com/index.html",
              "ex:mycustomfield": "my custom value"
            }
        """.trimMargin()
        println("|$jsonld|")
//        var dataset: Dataset
        // The parsers will do the necessary character set conversion.
        jsonld.byteInputStream().use { byteArrayInputStream ->
            val errorHandler = MyErrorHandler()
            val model = RDFParser
                .source(byteArrayInputStream)
                .lang(RDFLanguages.JSONLD)
                .errorHandler(errorHandler)
                .canonicalValues(true)
                .checking(true)
                .strict(true)
                .build()
                .toModel()
            println(model)
        }
        val model = ModelFactory.createDefaultModel().apply {
            setNsPrefix("oa", "http://www.w3.org/ns/oa#")
            setNsPrefix("dc", "http://purl.org/dc/elements/1.1/")
            setNsPrefix("dcterms", "http://purl.org/dc/terms/")
            setNsPrefix("dctypes", "http://purl.org/dc/dcmitype/")
            setNsPrefix("foaf", "http://xmlns.com/foaf/0.1/")
//            setNsPrefix("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#")
            setNsPrefix("rdfs", "http://www.w3.org/2000/01/rdf-schema#")
            setNsPrefix("skos", "http://www.w3.org/2004/02/skos/core#")
            setNsPrefix("xsd", "http://www.w3.org/2001/XMLSchema#")
            setNsPrefix("iana", "http://www.iana.org/assignments/relation/")
            setNsPrefix("owl", "http://www.w3.org/2002/07/owl#")
            setNsPrefix("as", "http://www.w3.org/ns/activitystreams#")
            setNsPrefix("schema", "http://schema.org/")
        }
        model.read(jsonld.byteInputStream(), "", "JSON-LD")
        println()
        model.write(System.out, "TTL")
        println()
        val g: DatasetGraph = DatasetFactory.wrap(model).asDatasetGraph()
        val ctx = JsonLDWriteContext()
        ctx.setFrame("""{"@type":"http://www.w3.org/ns/oa#Annotation"}""")
        RDFWriter.create()
            .format(RDFFormat.JSONLD_PRETTY)
            .source(g)
            .context(ctx)
            .build()
            .output(System.out)
    }

    class MyErrorHandler : ErrorHandler {
        override fun warning(p0: String?, p1: Long, p2: Long) {
            logger.warn { "warning at $p1,$p2: $p0" }
        }

        override fun error(p0: String?, p1: Long, p2: Long) {
            logger.error { "error at $p1,$p2: $p0" }
        }

        override fun fatal(p0: String?, p1: Long, p2: Long) {
            logger.fatal { "fatal at $p1,$p2: $p0" }
        }

    }
}