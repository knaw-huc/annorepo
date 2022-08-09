package nl.knaw.huc.annorepo.util

import com.google.common.collect.LinkedHashMultimap
import nl.knaw.huc.annorepo.client.AnnoRepoClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory

class ClassMethodsTest {
    private val log = LoggerFactory.getLogger(javaClass)

    @Test
    fun `extractVersion should return a version string when the given class is from a maven project`() {
        log.info("{}", javaClass.extractVersion())
        log.info("{}", AnnoRepoClient::class.java.extractVersion())

        val version = LinkedHashMultimap::class.java.extractVersion()
        log.info("{}", version)
        assertThat(version).isNotNull

        val version2 = LoggerFactory::class.java.extractVersion()
        log.info("{}", version2)
        assertThat(version2).isNotNull
    }

}