package nl.knaw.huc.annorepo

import org.junit.jupiter.api.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import nl.knaw.huc.annorepo.config.AnnoRepoConfiguration

class AnnoRepoConfigurationTest {
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    @Test
    fun `get base uri`() {
        val c = AnnoRepoConfiguration()
        log.info(c.externalBaseUrl)
    }

}