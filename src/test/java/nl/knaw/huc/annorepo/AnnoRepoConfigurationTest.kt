package nl.knaw.huc.annorepo

import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory

class AnnoRepoConfigurationTest {
    val log = LoggerFactory.getLogger(javaClass)

    @Test
    fun getBaseUri() {
        val c = AnnoRepoConfiguration()
        log.info(c.baseUri)
    }

    @Test
    fun getBaseUri2() {
        val c = AnnoRepoConfiguration()
        log.info(c.baseUri)
    }

    @Test
    fun getJdbcUri() {
        val c = AnnoRepoConfiguration()
        log.info(c.jdbcUri)
    }
}