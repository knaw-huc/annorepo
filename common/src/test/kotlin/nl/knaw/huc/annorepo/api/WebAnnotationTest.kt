package nl.knaw.huc.annorepo.api

import org.junit.jupiter.api.Test
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.ObjectWriter
import org.apache.logging.log4j.kotlin.logger
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType

internal class WebAnnotationTest {
    private val objectWriter: ObjectWriter = ObjectMapper().writerWithDefaultPrettyPrinter()

    @Test
    fun `template should return a map`() {
        val wa = WebAnnotation.template()
        assertThat(wa["type"]).isEqualTo("Annotation")
        logJsonSerialization(wa)
    }

    @Test
    fun `builder build should return WebAnnotationAsMap`() {
        val wa = WebAnnotation.Builder().withTarget("http://example.org/target-id").build()
        logger.info { "wa=$wa" }
        logJsonSerialization(wa)
    }

    @Test
    fun `builder build should throw an exception when no target is specified`() {
        assertThatExceptionOfType(MissingTargetException::class.java)
            .isThrownBy {
                val wa = WebAnnotation.Builder().build()
                logJsonSerialization(wa)
            }
            .withMessage("WebAnnotation must have 1 or more targets")

    }

    @Test
    fun `builder should accept string body and target`() {
        val wa = WebAnnotation.Builder()
            .withBody("http://example.org/body-id")
            .withTarget("http://example.org/target-id")
            .build()
        logger.info { "wa=$wa" }
        logJsonSerialization(wa)
    }

    @Test
    fun `builder should accept map body and target`() {
        val wa = WebAnnotation.Builder()
            .withBody(mapOf("type" to "Page"))
            .withTarget(
                mapOf(
                    "type" to "Image",
                    "source" to "http://example.org/image-id"
                )
            )
            .build()
        logger.info { "wa=$wa" }
        logJsonSerialization(wa)
    }

    private fun logJsonSerialization(wa: Any) {
        val asJson = objectWriter.writeValueAsString(wa)
        logger.info { "json=$asJson" }
    }

}