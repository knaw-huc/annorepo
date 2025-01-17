package nl.knaw.huc.annorepo

import java.nio.file.Path
import org.junit.jupiter.api.Test
import org.apache.logging.log4j.kotlin.logger
import org.assertj.core.api.Assertions.assertThat
import nl.knaw.huc.annorepo.api.ARConst
import nl.knaw.huc.annorepo.auth.SRAMClient
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

    @Test
    fun `text surfnet sram`() {
        val applicationToken = "AVcEmV8CnFpJpCctFuOJDfSIdSbpn5Mx6yq9lZxHadJI"
        val client = SRAMClient(applicationToken, "https://sram.surf.nl/api/tokens/introspect")
        val userToken = "AgUoYWfhf8O8xh-_Fua69cBIKJ9-tu7QXs3gywPyZB3w"
        val user = client.userForToken(userToken).fold(
            { e: SRAMClient.SramTokenError -> logger.info { e } },
            { u -> logger.info { u }; print(u.sramGroups) }
        )
    }
}
