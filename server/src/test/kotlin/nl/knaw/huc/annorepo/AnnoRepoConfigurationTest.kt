package nl.knaw.huc.annorepo

import java.io.File
import java.nio.file.Path
import kotlin.io.path.exists
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
        logger.info { c }
    }

    @OptIn(ExperimentalStdlibApi::class)
//    @Test
    fun `all AR_ env variables in config yml are also in ARConst EnvironmentVariable`() {
        val file = findFileInParentDirs("config.yml")
        val text = file.readText()
        val re = Regex("AR_[A-Z0-9_]+")
        val definedEnvs = ARConst.EnvironmentVariable.entries.map { it.name }.toSet()
        val foundEnvs = re.findAll(text).map { it.value }
        val undefinedEnvs = foundEnvs.filter { !definedEnvs.contains(it) }.toList()
        assertThat(undefinedEnvs).isEmpty()
    }

    private fun findFileInParentDirs(path: String): File {
        var wpath = Path.of(path)
        while (!wpath.exists()) {
            wpath = Path.of("""../$wpath""")
        }
        return wpath.toFile()
    }

}
