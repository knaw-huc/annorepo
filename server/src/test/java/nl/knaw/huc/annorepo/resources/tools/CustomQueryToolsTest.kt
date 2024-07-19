package nl.knaw.huc.annorepo.resources.tools

import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import nl.knaw.huc.annorepo.resources.tools.CustomQueryTools.CustomQueryCall
import nl.knaw.huc.annorepo.resources.tools.CustomQueryTools.extractParameterNames
import nl.knaw.huc.annorepo.resources.tools.CustomQueryTools.interpolate

class CustomQueryToolsTest {

    @Test
    fun `test encode and decode with parameters`() {
        val cqc = CustomQueryCall("queryName", mapOf("param1" to "value1", "param2" to "value2"))
        val encoded = CustomQueryTools.encode(cqc)
        assertEquals("queryName:param1=dmFsdWUx,param2=dmFsdWUy", encoded)

        val decoded = CustomQueryTools.decode(encoded).getOrThrow()
        assertEquals(cqc, decoded)
    }

    @Test
    fun `test encode and decode with just a name`() {
        val cqc = CustomQueryCall("queryName", mapOf())
        val encoded = CustomQueryTools.encode(cqc)
        assertEquals("queryName", encoded)

        val decoded = CustomQueryTools.decode(encoded).getOrThrow()
        assertEquals(cqc, decoded)
    }

    @Test
    fun `test decode with badly encoded parameter value`() {
        val encoded = "queryName:p1=AAAAAAA"
        val decoded = CustomQueryTools.decode(encoded)
        assertEquals(true, decoded.isFailure)
        val errorMessage = decoded.exceptionOrNull()?.message
        val expectedMessage = "bad Base64 value 'AAAAAAA' for parameter p1"
        assertEquals(expectedMessage, errorMessage)
    }

    @Test
    fun `test decode`() {
        val encoded = "queryName:type=UmVzb2x1dGlvbg=="
        val decoded = CustomQueryTools.decode(encoded).getOrThrow()
        val expected = CustomQueryCall("queryName", mapOf("type" to "Resolution"))
        assertEquals(expected, decoded)
    }

    @Test
    fun `test parameter interpolation in template query`() {
        val template = """{"body.type":"<type>"}"""
        val expected = """{"body.type":"rp:Resolution"}"""
        val parameters = mapOf("type" to "rp:Resolution")
        val expanded = template.interpolate(parameters)
        assertEquals(expected, expanded)
    }

    @Test
    fun `test special json characters are escaped in parameter interpolation`() {
        val template = """{"body.type":"<type>"}"""
        val expected = """{"body.type":"bla\";drop database;"}"""
        val parameters = mapOf("type" to """bla";drop database;""")
        val expanded = template.interpolate(parameters)
        assertEquals(expected, expanded)
    }

    @Test
    fun `test parameterName extraction from template with parameters`() {
        val template = """{
            |"body.type": "<type>",
            |"target.source": "<target_source>"
            |}""".trimMargin()
        val parameterNames = template.extractParameterNames()
        val expected = listOf("type", "target_source")
        assertEquals(expected, parameterNames)
    }

    @Test
    fun `test parameterName extraction from template without parameters`() {
        val template = """{
            |"body.type": "Page"
            |}""".trimMargin()
        val parameterNames = template.extractParameterNames()
        val expected = emptyList<String>()
        assertEquals(expected, parameterNames)
    }

}