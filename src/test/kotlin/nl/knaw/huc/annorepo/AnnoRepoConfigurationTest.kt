package nl.knaw.huc.annorepo

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

    @Test
    fun getBaseUri2() {
        val c = AnnoRepoConfiguration()
        log.info(c.externalBaseUrl)
    }

}