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
}