package nl.knaw.huc.annorepo.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.ObjectWriter
import com.github.ajalt.mordant.rendering.TextColors.blue
import com.github.ajalt.mordant.rendering.TextColors.green
import com.github.ajalt.mordant.rendering.TextColors.red
import com.github.ajalt.mordant.rendering.TextColors.yellow
import com.github.ajalt.mordant.table.table
import com.github.ajalt.mordant.terminal.Terminal
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.required
import nl.knaw.huc.annorepo.client.AnnoRepoClient
import java.net.URI
import javax.ws.rs.core.EntityTag

class IntegrationTest {

    fun testServer(server: String) {
        val t = Terminal()
        t.println("Testing server at $server :")
        t.println()
        val client = AnnoRepoClient(URI.create(server), "annorepo-integration-tester")
        val testResults = mutableMapOf<String, Boolean>()

        testResults["testAbout"] = client.testAbout(t)
        testResults["ContainerCreationAndDeletion"] = client.testContainerCreationAndDeletion(t)
        testResults["container field count"] = client.testContainerFieldCount(t)
        testResults["batch upload"] = client.testBatchUpload(t)
        testResults["this test should fail"] = client.testFailure(t)

        t.println("Results:")
        t.printTable(testResults)
        t.println("done!")
    }

    private fun AnnoRepoClient.testAbout(t: Terminal): Boolean =
        runTest(t, "Testing /about") {
            val about = getAbout()
            t.printJson(about)
            t.printAssertion("/about info should have a version field", about.version.isNotBlank())
        }

    private fun AnnoRepoClient.testContainerCreationAndDeletion(t: Terminal): Boolean =
        runTest(t, "Testing creating and deleting a container") {
            inTemporaryContainer(this, t) { }
        }

    private fun AnnoRepoClient.testFailure(t: Terminal): Boolean =
        runTest(t, "A failing test") {
            throw Exception("Don't worry, this should happen")
        }

    private fun AnnoRepoClient.testBatchUpload(t: Terminal): Boolean =
        runTest(t, "Testing the batch upload") {
            inTemporaryContainer(this, t) { containerName ->
                val annotations = listOf(
                    mapOf(
                        "body" to "urn:example:body1",
                        "target" to "urn:example:target1"
                    ),
                    mapOf(
                        "body" to "urn:example:body2",
                        "target" to "urn:example:target2"
                    )
                )
                t.printStep("Batch uploading annotations")
                t.printJson(annotations)
                val results = this.batchUpload(containerName, annotations)

                val fc = getFieldCount(containerName)
//                t.println(green(fc1.toString()))

                t.printAssertion(
                    "fieldCounts should have body = 2",
                    fc.getOrDefault("body", 0) == 2
                )
                t.printAssertion(
                    "fieldCounts should have target = 2",
                    fc.getOrDefault("target", 0) == 2
                )

                t.printStep("Deleting annotations")
                for (annotationData in results.annotationData) {
                    val dar = deleteAnnotation(
                        containerName = annotationData.containerName,
                        annotationName = annotationData.annotationName,
                        eTag = EntityTag(annotationData.etag, true).toString()
                    )
                }

                val fc2 = getFieldCount(containerName)
                t.printAssertion("fieldCounts should be empty", fc2.isEmpty())

            }
        }

    private val jsonWriter: ObjectWriter = ObjectMapper().writerWithDefaultPrettyPrinter()

    private fun AnnoRepoClient.testContainerFieldCount(t: Terminal): Boolean =
        runTest(t, "Testing the annotation field counter") {
            inTemporaryContainer(this, t) { containerName ->
                val fc0 = getFieldCount(containerName)
                t.printAssertion("Initially, fieldCounts should be empty", fc0.isEmpty())

                val annotation: Map<String, Any> = mapOf("body" to mapOf("id" to "urn:example:blahblahblah"))
                t.printStep("Adding annotation with body.id field: ")
                t.printJson(annotation)
                val car = createAnnotation(containerName, annotation)
//                t.println(green(car.toString()))

                val fc1 = getFieldCount(containerName)
//                t.println(green(fc1.toString()))
                t.printAssertion(
                    "fieldCounts should have body.id = 1",
                    fc1.getOrDefault("body.id", 0) == 1
                )

                val newAnnotation: Map<String, Any> = mapOf("body" to "urn:example:blahblahblah")
                t.printStep("Updating the annotation: ")
                t.printJson(newAnnotation)
                val uar = updateAnnotation(containerName, car.containerId, car.eTag, newAnnotation)

                val fc2 = getFieldCount(containerName)
                t.printAssertion(
                    "fieldCounts should not have a body.id field",
                    !fc2.containsKey("body.id")
                )
                t.printAssertion(
                    "fieldCounts should have body = 1",
                    fc2.getOrDefault("body", 0) == 1
                )

                t.printStep("Deleting the annotation")
                val dr = deleteAnnotation(containerName, uar.containerId, uar.eTag)
//                t.println(dr)

                val fc3 = getFieldCount(containerName)
                t.printAssertion("fieldCounts should be empty", fc3.isEmpty())
            }
        }

    private fun Terminal.printAssertion(message: String, assertion: Boolean) =
        println("- $message: ${assertion.asCheckMark()}")

    private fun Terminal.printStep(message: String) =
        println("> $message")

    private fun Terminal.printJson(obj: Any) =
        println(blue(jsonWriter.writeValueAsString(obj)))

    private fun Terminal.printTable(testResults: MutableMap<String, Boolean>) {
//        val rows = testResults.map { name,result -> row(name, failureOrSuccess(result)) }
        println(table {
            header { row("Test", "Result") }
            body {
                testResults.forEach { (testName, success) -> row(yellow(testName), success.asResult()) }
            }
        })
    }

    private fun runTest(t: Terminal, title: String, testFunction: () -> Unit): Boolean =
        try {
            t.println(yellow(title))
            testFunction()
            t.println()
            true
        } catch (e: Exception) {
            t.println()
            e.printStackTrace()
            false
        }

    private fun inTemporaryContainer(
        ac: AnnoRepoClient,
        t: Terminal,
        containerName: String? = "tmp-container",
        func: (containerName: String) -> Unit?
    ) {
        t.printStep("Creating container $containerName")
        val r = ac.createContainer(containerName)
        try {
            func(r.containerId)
        } finally {
            t.printStep("Deleting container $containerName")
            val deleteResult = ac.deleteContainer(r.containerId, eTag = r.eTag)
        }
    }

    private fun Boolean.asResult(): String =
        when (this) {
            true -> green("success")
            false -> red("failure")
        }

    private fun Boolean.asCheckMark(): String =
        when (this) {
            true -> green("✔")
            false -> red("✘")
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

