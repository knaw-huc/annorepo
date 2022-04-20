package nl.knaw.huc.annorepo.api

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.*

internal class ContainerDataTest {
    private val log = LoggerFactory.getLogger(javaClass)

    @Test
    fun test_default_constructor() {
        val cd = ContainerData()
        log.info("cd=$cd")
        assertThat(cd).isNotNull
    }

    @Test
    fun test_secondary_constructor() {
        val cd = ContainerData(1, "name", Date.from(Instant.now()), Date.from(Instant.now()))
        log.info("cd=$cd")
        assertThat(cd).isNotNull
        assertThat(cd.id).isEqualTo(1)
        assertThat(cd.name).isEqualTo("name")
    }
}