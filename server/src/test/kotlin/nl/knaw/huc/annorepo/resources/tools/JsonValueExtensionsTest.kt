package nl.knaw.huc.annorepo.resources.tools

import java.io.StringReader
import jakarta.json.Json
import jakarta.json.JsonValue
import org.junit.jupiter.api.Test
import org.assertj.core.api.Assertions.assertThat
import org.slf4j.LoggerFactory

class JsonValueExtensionsTest {
    private val log = LoggerFactory.getLogger(this.javaClass)

    @Test
    fun `simplifying a complex JsonValue works as expected`() {
        val json = """
            {
                "bool1": true,
                "bool2":false,
                "string": "Hello World",
                "int": 2023,
                "float": 3.1415926,
                "arrayOfStrings": ["Hello","World"],
                "arrayOfInts" : [3,1,4,1,5,9,2,6],
                "object": {
                    "field1": 1,
                    "field2": "bla",
                    "field3": true
                }                
            }
        """.trimIndent()
        val jsonValue: JsonValue = Json.createReader(StringReader(json)).readValue()
        log.info("{}", jsonValue)
        val simpleValue = jsonValue.toSimpleValue()
        log.info("{}", simpleValue)
        assertThat(simpleValue is Map<*, *>).isTrue

        val map = simpleValue as Map<*, *>
        val arrayOfStrings = map["arrayOfStrings"]
        log.info("{}", arrayOfStrings)
        assertThat(arrayOfStrings.isStringArray()).isTrue

        val arrayOfInts = map["arrayOfInts"]
        log.info("{}", arrayOfInts)
        assertThat(arrayOfInts.isNumberArray()).isTrue
    }

    private fun Any?.isNumberArray() =
        this is Array<*>
                && all { it is Number }

    private fun Any?.isStringArray() =
        this is Array<*>
                && all { it is String }
}