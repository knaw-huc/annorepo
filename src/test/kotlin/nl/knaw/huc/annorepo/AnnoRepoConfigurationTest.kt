package nl.knaw.huc.annorepo

import nl.knaw.huc.annorepo.config.AnnoRepoConfiguration
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class AnnoRepoConfigurationTest {
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    @Test
    fun getBaseUri() {
        val c = AnnoRepoConfiguration()
        log.info(c.externalBaseUrl)
    }

}