package nl.knaw.huc.annorepo.api

import org.junit.jupiter.api.Test
import org.assertj.core.api.Assertions.assertThat

class ExtensionsTest {
    @Test
    fun `these names are not valid`() {
        val invalidNames = listOf(
            "spaces are not allowed in annotation names",
            "behold:a_name"
        )
        invalidNames.forEach {
            assertThat(it.isValidAnnotationName()).withFailMessage { "$it should be an invalid name" }.isFalse()
            assertThat(it.isValidContainerName()).withFailMessage { "$it should be an invalid name" }.isFalse()
        }
    }

    @Test
    fun `these names are valid`() {
        val validNames = listOf(
            "spacesarenotallowedinannotationnames",
            "behold-a_name"
        )
        validNames.forEach {
            assertThat(it.isValidAnnotationName()).withFailMessage { "$it should be a valid name" }.isTrue()
            assertThat(it.isValidContainerName()).withFailMessage { "$it should be a valid name" }.isTrue()
        }
    }

    @Test
    fun `getNestedValue returns the correct nested value if it exists`() {
        val map = mapOf("a" to mapOf("b" to "c"))
        val abValue = map.getNestedValue<String>("a.b")
        assertThat(abValue).isEqualTo("c")
    }

    @Test
    fun `getNestedValue returns null if any of the fields does not exist`() {
        val map = mapOf("a" to mapOf("b" to "c"))
        val abValue = map.getNestedValue<String>("b.a")
        assertThat(abValue).isNull()
    }

//    @Test
//    fun `getNestedValue returns null if the value exist, but can't be cast to the desired class`() {
//        val map = mapOf("a" to mapOf("b" to "c"))
//        val abValue = map.getNestedValue<Long>("a.b")
//        assertThat(abValue).isNull()
//    }

}