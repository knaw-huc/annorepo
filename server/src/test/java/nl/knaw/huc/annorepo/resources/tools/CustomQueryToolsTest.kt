package nl.knaw.huc.annorepo.resources.tools

import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import nl.knaw.huc.annorepo.resources.tools.CustomQueryTools.CustomQueryCall

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
    fun `test query template expansion`() {
        val template = """{"body.type":"<type>"}"""
        val expected = """{"body.type":"rp:Resolution"}"""
        val parameters = mapOf("type" to "rp:Resolution")
        val expanded = CustomQueryTools.expandQueryTemplate(template, parameters)
        assertEquals(expected, expanded)
    }

    @Test
    fun `test special json characters are escaped in query template expansion`() {
        val template = """{"body.type":"<type>"}"""
        val expected = """{"body.type":"bla\";drop database;"}"""
        val parameters = mapOf("type" to """bla";drop database;""")
        val expanded = CustomQueryTools.expandQueryTemplate(template, parameters)
        assertEquals(expected, expanded)
    }

}