package nl.knaw.huc.annorepo

import java.nio.file.Path
import org.junit.jupiter.api.Test
import org.apache.logging.log4j.kotlin.logger
import org.assertj.core.api.Assertions.assertThat
import nl.knaw.huc.annorepo.api.ARConst
import nl.knaw.huc.annorepo.config.AnnoRepoConfiguration

class AnnoRepoConfigurationTest {

    @Test
    fun `get base uri`() {
        val c = AnnoRepoConfiguration()
        logger.info { c.externalBaseUrl }
        assertThat(c).isNotNull
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun `all AR_ env variables in config yml are also in ARConst EnvironmentVariable`() {
        val text = Path.of("../config.yml").toFile().readText()
        val re = Regex("AR_[A-Z0-9_]+")
        val definedEnvs = ARConst.EnvironmentVariable.entries.map { it.name }.toSet()
        val foundEnvs = re.findAll(text).map { it.value }
        val undefinedEnvs = foundEnvs.filter { !definedEnvs.contains(it) }.toList()
        assertThat(undefinedEnvs).isEmpty()
    }

}
