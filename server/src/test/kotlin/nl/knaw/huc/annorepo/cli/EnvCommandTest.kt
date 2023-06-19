package nl.knaw.huc.annorepo.cli

import org.junit.jupiter.api.Test
import io.dropwizard.core.setup.Bootstrap
import net.sourceforge.argparse4j.inf.Namespace
import org.mockito.Mockito.mock

internal class EnvCommandTest {

    @Test
    fun `running the env command returns the defined AR_ environment variables`() {
        val ec = EnvCommand()
        val bootstrap = mock(
            Bootstrap::class.java
        )
        val namespace = mock(Namespace::class.java)
        ec.run(bootstrap = bootstrap, namespace = namespace)
    }
}