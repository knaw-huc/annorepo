package nl.knaw.huc.annorepo.resources.tools

import jakarta.ws.rs.BadRequestException
import kotlin.test.assertNotNull
import org.junit.jupiter.api.Test
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.bson.Document
import nl.knaw.huc.annorepo.api.ANNO_JSONLD_URL

class AssertAnnotationHasValidContextTest {
    @Test
    fun `annotation with valid context should pass`() {
        val json = """{
            |"@context": [
            |   "$ANNO_JSONLD_URL",
            |   "http://example.org/mycontext.jsonld",
            |   { "ns": "https://example.org/namespace#"}
            |],
            |"type": "Annotation",
            |"body": "body",
            |"target": "target"
            |}""".trimMargin()
        val doc = Document.parse(json).apply { validate() }
        assertNotNull(doc)
    }

    @Test
    fun `annotation without context should throw exception`() {
        val json = """{
            |"type": "Annotation",
            |"body": "body",
            |"target": "target"
            |}""".trimMargin()
        assertThatThrownBy {
            Document.parse(json).apply { validate() }
        }.isInstanceOf(BadRequestException::class.java)
            .hasMessage(
                "Invalid input:\nThe Annotation MUST have 1 or more @context values.\n" +
                        "see https://www.w3.org/TR/annotation-model/#annotations"
            )
    }

    @Test
    fun `annotation with invalid string context should throw exception`() {
        val json = """{
            |"@context": "",
            |"type": "Annotation",
            |"body": "body",
            |"target": "target"
            |}""".trimMargin()
        assertThatThrownBy {
            Document.parse(json).apply { validate() }
        }.isInstanceOf(BadRequestException::class.java)
            .hasMessage(
                "Invalid input:\nThe Annotation MUST have 1 or more @context values " +
                        "and http://www.w3.org/ns/anno.jsonld MUST be one of them.\n" +
                        "see https://www.w3.org/TR/annotation-model/#annotations"
            )
    }

    @Test
    fun `annotation with invalid list context should throw exception`() {
        val json = """{
            |"@context": [
            |   "http://example.org/mycontext.jsonld",
            |   { "ns": "https://example.org/namespace#"}
            |],
            |"type": "Annotation",
            |"body": "body",
            |"target": "target"
            |}""".trimMargin()
        assertThatThrownBy {
            Document.parse(json).apply { validate() }
        }.isInstanceOf(BadRequestException::class.java)
            .hasMessage(
                "Invalid input:\nThe Annotation MUST have 1 or more @context values " +
                        "and http://www.w3.org/ns/anno.jsonld MUST be one of them.\n" +
                        "see https://www.w3.org/TR/annotation-model/#annotations"
            )
    }

    @Test
    fun `annotation with invalid object context should throw exception`() {
        val json = """{
            |"@context": 
            |   { "ns": "https://example.org/namespace#"}
            |,
            |"body": "body",
            |"target": "target"
            |}""".trimMargin()
        assertThatThrownBy {
            Document.parse(json).apply { validate() }
        }.isInstanceOf(BadRequestException::class.java)
            .hasMessage(
                """Invalid input:
                |invalid @context.
                |An Annotation MUST have 1 or more types.
                |see https://www.w3.org/TR/annotation-model/#annotations""".trimMargin()
            )
    }

    @Test
    fun `annotation without target should throw exception`() {
        val json = """{
            |"@context": "$ANNO_JSONLD_URL",
            |"type": "Annotation",
            |"body": "body",
            |}""".trimMargin()
        assertThatThrownBy {
            Document.parse(json).apply { validate() }
        }.isInstanceOf(BadRequestException::class.java)
            .hasMessage(
                "Invalid input:\nThere MUST be 1 or more target relationships associated with an Annotation.\n" +
                        "see https://www.w3.org/TR/annotation-model/#annotations"
            )
    }

    @Test
    fun `annotation without type should throw exception`() {
        val json = """{
            |"@context": "$ANNO_JSONLD_URL",
            |"body": "body",
            |"target": "target",
            |}""".trimMargin()
        assertThatThrownBy {
            Document.parse(json).apply { validate() }
        }.isInstanceOf(BadRequestException::class.java)
            .hasMessage(
                "Invalid input:\nAn Annotation MUST have 1 or more types.\n" +
                        "see https://www.w3.org/TR/annotation-model/#annotations"
            )
    }

    @Test
    fun `annotation without Annotation in type should throw exception`() {
        val json = """{
            |"@context": "$ANNO_JSONLD_URL",
            |"type": "Something",
            |"body": "body",
            |"target": "target",
            |}""".trimMargin()
        assertThatThrownBy {
            Document.parse(json).apply { validate() }
        }.isInstanceOf(BadRequestException::class.java)
            .hasMessage(
                "Invalid input:\nAn Annotation MUST have 1 or more types, " +
                        "and the Annotation class MUST be one of them.\n" +
                        "see https://www.w3.org/TR/annotation-model/#annotations"
            )
    }

    @Test
    fun `annotation with missing Annotation in type should throw exception`() {
        val json = """{
            |"@context": "$ANNO_JSONLD_URL",
            |"type": ["Something","Something else"],
            |"body": "body",
            |"target": "target",
            |}""".trimMargin()
        assertThatThrownBy {
            Document.parse(json).apply { validate() }
        }.isInstanceOf(BadRequestException::class.java)
            .hasMessage(
                "Invalid input:\nAn Annotation MUST have 1 or more types, " +
                        "and the Annotation class MUST be one of them.\n" +
                        "see https://www.w3.org/TR/annotation-model/#annotations"
            )
    }

    @Test
    fun `empty annotation should throw exception`() {
        val json = """{}"""
        assertThatThrownBy {
            Document.parse(json).apply { validate() }
        }.isInstanceOf(BadRequestException::class.java)
            .hasMessage(
                """Invalid input:
                |The Annotation MUST have 1 or more @context values.
                |An Annotation MUST have 1 or more types.
                |There MUST be 1 or more target relationships associated with an Annotation.
                |see https://www.w3.org/TR/annotation-model/#annotations""".trimMargin()
            )
    }
}