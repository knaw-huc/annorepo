package nl.knaw.huc.annorepo.api

import java.time.Instant
import java.util.Date
import org.junit.jupiter.api.Test
import org.assertj.core.api.Assertions.assertThat
import org.slf4j.LoggerFactory

internal class ContainerDataTest {
    private val log = LoggerFactory.getLogger(javaClass)

    @Test
    fun `test default constructor`() {
        val cd = ContainerData()
        log.info("cd=$cd")
        assertThat(cd).isNotNull
    }

    @Test
    fun `test secondary constructor`() {
        val cd = ContainerData(1, "name", Date.from(Instant.now()), Date.from(Instant.now()))
        log.info("cd=$cd")
        assertThat(cd).isNotNull
        assertThat(cd.id).isEqualTo(1)
        assertThat(cd.name).isEqualTo("name")
    }
}