package nl.knaw.huc.annorepo.api

import org.junit.jupiter.api.Test
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.ObjectWriter
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Fail.fail
import org.slf4j.LoggerFactory

internal class WebAnnotationTest {
    private val log = LoggerFactory.getLogger(javaClass)
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
        log.info("wa={}", wa)
        logJsonSerialization(wa)
    }

    @Test
    fun `builder build should throw an exception when no target is specified`() {
        try {
            val wa = WebAnnotation.Builder().build()
            logJsonSerialization(wa)
            fail<String>("Expected a MissingTargetException")
        } catch (e: MissingTargetException) {
            assertThat(e).isNotNull
        }
    }

    @Test
    fun `builder should accept string body and target`() {
        val wa = WebAnnotation.Builder()
            .withBody("http://example.org/body-id")
            .withTarget("http://example.org/target-id")
            .build()
        log.info("wa={}", wa)
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
        log.info("wa={}", wa)
        logJsonSerialization(wa)
    }

    private fun logJsonSerialization(wa: Any) {
        val asJson = objectWriter.writeValueAsString(wa)
        log.info("json={}", asJson)
    }

}