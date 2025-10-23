package nl.knaw.huc.annorepo.resources.tools

import java.io.StringReader
import jakarta.json.Json
import jakarta.json.JsonValue
import kotlin.io.path.Path
import kotlin.io.path.readText
import org.junit.jupiter.api.Test
import org.apache.logging.log4j.kotlin.logger
import org.assertj.core.api.Assertions.assertThat

class JsonValueExtensionsTest {

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
        logger.info { jsonValue }
        val simpleValue = jsonValue.toSimpleValue()
        logger.info { simpleValue }
        assertThat(simpleValue is Map<*, *>).isTrue

        val map = simpleValue as Map<*, *>
        val arrayOfStrings = map["arrayOfStrings"]
        logger.info { arrayOfStrings }
        assertThat(arrayOfStrings.isStringArray()).isTrue

        val arrayOfInts = map["arrayOfInts"]
        logger.info { arrayOfInts }
        assertThat(arrayOfInts.isNumberArray()).isTrue
    }

//    @Test
    fun `read chunk`() {
        val path = "/Users/bram/workspaces/annorepo/annorepo/chunk.json"
        val json = Path(path).readText()
        val value = Json.createReader(StringReader(json)).readValue()
        if (value.valueType != JsonValue.ValueType.ARRAY) {
            logger.error { "expected an array of web annotations, got a ${value.valueType}" }
        }
        val array = value.asJsonArray()
        array.forEachIndexed { i, value ->
            if (value.valueType != JsonValue.ValueType.OBJECT) {
                logger.error { "item $i: expected a web annotation, got a ${value.valueType}" }
            }
        }
        println(array.first())
        println(array.last())
    }

    private fun Any?.isNumberArray() =
        this is Array<*>
                && all { it is Number }

    private fun Any?.isStringArray() =
        this is Array<*>
                && all { it is String }
}