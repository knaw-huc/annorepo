package nl.knaw.huc.annorepo.api

import org.junit.jupiter.api.Test
import org.assertj.core.api.Assertions.assertThat

class PropertySetTest {
    @Test
    fun `test required with multiple keys`() {
        val propertySet: PropertySet = mapOf(
            "a" to mapOf(
                "b" to mapOf(
                    "c" to "d"
                )
            )
        )
        assertThat(propertySet.required<String>("a", "b", "c")).isEqualTo("d")
    }

    @Test
    fun `test existing optional with multiple keys`() {
        val propertySet: PropertySet = mapOf(
            "a" to mapOf(
                "b" to mapOf(
                    "c" to "d"
                )
            )
        )
        assertThat(propertySet.optional<String>("a", "b", "c")).isEqualTo("d")
    }

    @Test
    fun `test missing optional with multiple keys`() {
        val propertySet: PropertySet = mapOf(
            "a" to mapOf(
                "b" to mapOf(
                    "c" to "d"
                )
            )
        )
        assertThat(propertySet.optional<String>("a", "b", "d")).isEqualTo(null)
    }

    @Test
    fun `test missing optional with multiple keys, different hierarchy`() {
        val propertySet: PropertySet = mapOf(
            "a" to mapOf(
                "b1" to mapOf(
                    "d" to "e"
                )
            )
        )
        assertThat(propertySet.optional<String>("a", "b", "d")).isEqualTo(null)
    }
}