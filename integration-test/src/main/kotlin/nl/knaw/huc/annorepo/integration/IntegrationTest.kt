package nl.knaw.huc.annorepo.integration

import com.github.ajalt.mordant.rendering.TextColors.green
import com.github.ajalt.mordant.rendering.TextColors.red
import com.github.ajalt.mordant.table.table
import com.github.ajalt.mordant.terminal.Terminal
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.required
import nl.knaw.huc.annorepo.client.AnnoRepoClient
import java.net.URI

class IntegrationTest {

    fun testServer(server: String) {
        val t = Terminal()
        t.println("Testing server at $server :")
        val client = AnnoRepoClient(URI.create(server), "annorepo-integration-tester")
        val about = client.getAbout()
        t.println(green(about.toString()))
        val containerName = "testcontainer"
        val r = client.createContainer(containerName)
        t.println(green(r.toString()))
        val deleteResult = client.deleteContainer(r.containerId, etag = r.eTag)
        t.println(green(deleteResult.toString()))
//        t.printTable()
        t.println("done!")
    }

    private fun Terminal.printTable() {
        println(table {
            header { row("Test", "Result") }
            body {
                row("1", red("failure"))
                row("2", green("success"))
            }
        })
    }

    companion object {
        @Throws(Exception::class)
        @JvmStatic
        fun main(args: Array<String>) {
            val parser = ArgParser("annorepo-test-server")
            val server by parser.option(
                ArgType.String,
                shortName = "s",
                description = "URL of the AnnoRepo server to test."
            ).required()
            parser.parse(args)
            IntegrationTest().testServer(server)
        }
    }
}