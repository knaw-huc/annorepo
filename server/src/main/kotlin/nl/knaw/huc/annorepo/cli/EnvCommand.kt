package nl.knaw.huc.annorepo.cli

import io.dropwizard.core.cli.Command
import io.dropwizard.core.setup.Bootstrap
import net.sourceforge.argparse4j.inf.Namespace
import net.sourceforge.argparse4j.inf.Subparser
import nl.knaw.huc.annorepo.api.ARConst

@OptIn(ExperimentalStdlibApi::class)
class EnvCommand : Command("env", "Shows the environment variables you can use") {
    override fun configure(subparser: Subparser?) {

    }

    override fun run(bootstrap: Bootstrap<*>?, namespace: Namespace?) {
        ARConst.EnvironmentVariable.entries.toTypedArray().sorted().forEach { v ->
            println(v.name)
        }
    }
}