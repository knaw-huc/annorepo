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
        val testResults = mutableMapOf<String, Boolean>()

        testResults["testAbout"] = client.testAbout(t)
        testResults["ContainerCreationAndDeletion"] = client.testContainerCreationAndDeletion(t)

        t.printTable(testResults)
        t.println("done!")
    }

    private fun AnnoRepoClient.testContainerCreationAndDeletion(
        t: Terminal
    ): Boolean {
        val containerName = "testcontainer"
        val r = createContainer(containerName)
        t.println(green(r.toString()))
        val deleteResult = deleteContainer(r.containerId, etag = r.eTag)
        t.println(green(deleteResult.toString()))
        return true
    }

    private fun AnnoRepoClient.testAbout(
        t: Terminal
    ): Boolean {
        val about = getAbout()
        t.println(green(about.toString()))
        return true
    }

    private fun Terminal.printTable(testResults: MutableMap<String, Boolean>) {
//        val rows = testResults.map { name,result -> row(name, failureOrSuccess(result)) }
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