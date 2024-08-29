package nl.knaw.huc.annorepo.api

import java.time.Instant
import java.util.Date
import org.junit.jupiter.api.Test
import org.apache.logging.log4j.kotlin.logger
import org.assertj.core.api.Assertions.assertThat

internal class ContainerDataTest {

    @Test
    fun `test default constructor`() {
        val cd = ContainerData()
        logger.info { "cd=$cd" }
        assertThat(cd).isNotNull
    }

    @Test
    fun `test secondary constructor`() {
        val cd = ContainerData(1, "name", Date.from(Instant.now()), Date.from(Instant.now()))
        logger.info { "cd=$cd" }
        assertThat(cd).isNotNull
        assertThat(cd.id).isEqualTo(1)
        assertThat(cd.name).isEqualTo("name")
    }
}