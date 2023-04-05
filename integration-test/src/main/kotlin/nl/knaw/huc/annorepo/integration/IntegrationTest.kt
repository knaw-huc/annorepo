package nl.knaw.huc.annorepo.integration

import java.net.URI
import jakarta.ws.rs.core.EntityTag
import arrow.core.Either
import arrow.core.getOrElse
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.ObjectWriter
import com.github.ajalt.mordant.rendering.TextColors.*
import com.github.ajalt.mordant.table.table
import com.github.ajalt.mordant.terminal.Terminal
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.required
import nl.knaw.huc.annorepo.api.IndexType
import nl.knaw.huc.annorepo.api.UserEntry
import nl.knaw.huc.annorepo.client.ARResult
import nl.knaw.huc.annorepo.client.AnnoRepoClient
import nl.knaw.huc.annorepo.client.FilterContainerAnnotationsResult
import nl.knaw.huc.annorepo.client.RequestError

class IntegrationTest {
    private val jsonWriter: ObjectWriter = ObjectMapper().writerWithDefaultPrettyPrinter()

    fun testServer(serverURI: String, apiKey: String?) {
        val t = Terminal()
        t.println("Testing server at $serverURI :")
        t.println()
        val client = AnnoRepoClient(
            serverURI = URI.create(serverURI),
            apiKey = apiKey,
            userAgent = "annorepo-integration-tester"
        )
        val testResults = mutableMapOf<String, Boolean>()

        testResults["testAbout"] = client.testAbout(t)
        testResults["ContainerCreationAndDeletion"] = client.testContainerCreationAndDeletion(t)
        testResults["container field count"] = client.testContainerFieldCount(t)
        testResults["batch upload"] = client.testBatchUpload(t)
        testResults["admin endpoints"] = client.testAdminEndpoints(t)
        testResults["this test should fail"] = testFailure(t)

        t.println("Results:")
        t.printTable(testResults)
        t.println("done!")
    }

    private fun AnnoRepoClient.testAbout(t: Terminal): Boolean =
        runTest(t, "Testing /about") {
            getAbout().thenAssertResult(t) { aboutResult ->
                val aboutInfo = aboutResult.aboutInfo
                val passed = MyBool(true)
                t.printJson(aboutInfo)
                t.printAssertion("/about info should have a version field", aboutInfo.version.isNotBlank())
                passed.value
            }
        }

    private fun <T> Either<RequestError, T>.thenAssertResult(
        t: Terminal,
        successFunction: (T) -> Boolean,
    ): Boolean =
        fold(
            { error -> t.printError(error.message); return false },
            successFunction
        )

    private fun AnnoRepoClient.testContainerCreationAndDeletion(t: Terminal): Boolean =
        runTest(t, "Testing creating and deleting a container") {
            inTemporaryContainer(this, t) { }
        }

    private fun testFailure(t: Terminal): Boolean =
        runTest(t, "A failing test") {
            throw Exception("This exception was intentionally thrown.")
        }

    private fun AnnoRepoClient.testBatchUpload(t: Terminal): Boolean =
        runTest(t, "Testing the batch upload") {
            inTemporaryContainer(this, t) { containerName ->
                val batchSize = 314
                val annotations = mutableListOf<Map<String, Any>>()
                for (i in 1..batchSize) {
                    annotations.add(
                        mapOf(
                            "type" to "Annotation",
                            "body" to mapOf(
                                "type" to "Page", "id" to "urn:example:body$i"
                            ),
                            "target" to "urn:example:target$i"
                        )
                    )
                }
                t.printStep("Batch uploading annotations")
//                t.printJson(annotations)
                val results: ARResult.BatchUploadResult =
                    this.batchUpload(containerName, annotations).getOrElse { throw Exception() }

                val fc = getFieldInfo(containerName).getOrElse { throw Exception() }
//                t.println(green(fc1.toString()))

                t.printAssertion(
                    "fieldCounts should have body.id = $batchSize",
                    fc.fieldInfo.getOrDefault("body.id", 0) == batchSize
                )
                t.printAssertion(
                    "fieldCounts should have target = $batchSize",
                    fc.fieldInfo.getOrDefault("target", 0) == batchSize
                )

                t.printStep("Search for body.id = urn:example:body42")
                val query = mapOf("body.id" to "urn:example:body42")
                val createSearchResult: ARResult.CreateSearchResult =
                    this.createSearch(containerName = containerName, query = query)
                        .getOrElse { throw Exception() }

                val resultPageResult = this.getSearchResultPage(
                    containerName = containerName,
                    queryId = createSearchResult.queryId,
                    page = 0
                )
                t.println(resultPageResult)

                t.printStep("get search info")
                val getSearchInfoResult = this.getSearchInfo(
                    containerName = containerName,
                    queryId = createSearchResult.queryId
                )
                t.println(getSearchInfoResult)

                t.printStep("get container")
                val getContainerResult = this.getContainer(containerName)
                t.println(getContainerResult)

                t.printStep("get container metadata")
                val getContainerMetadataResult = this.getContainerMetadata(containerName)
                t.println(getContainerMetadataResult)

                t.printStep("add index")
                val addIndexResult = this.addIndex(containerName, "body", IndexType.HASHED)
                t.println(addIndexResult)

                t.printStep("get index")
                val getIndexResult = this.getIndex(containerName, "body", IndexType.HASHED)
                t.println(getIndexResult)

                t.printStep("list indexes")
                val listIndexResult = this.listIndexes(containerName)
                t.println(listIndexResult)

                t.printStep("delete index")
                val deleteIndexResult = this.deleteIndex(containerName, "body", IndexType.HASHED)
                t.println(deleteIndexResult)

                t.printStep("using filterContainerAnnotations")
                val query2 = mapOf("body.type" to "Page")
                val filterContainerAnnotationsResult: FilterContainerAnnotationsResult? =
                    this.filterContainerAnnotations(containerName, query2).getOrNull()
                filterContainerAnnotationsResult?.let {
                    it.annotations.forEach { item ->
                        item.fold(
                            { error: RequestError -> print(error) },
                            { annotation: String -> print(annotation) }
                        )
                    }
                }

                t.printStep("Deleting annotations")
                var annotationsDeleted = 0
                for (annotationData in results.annotationData) {
                    deleteAnnotation(
                        containerName = annotationData.containerName,
                        annotationName = annotationData.annotationName,
                        eTag = EntityTag(annotationData.eTag, true).value
                    ).thenAssertResult(t) {
                        annotationsDeleted += 1
                        true
                    }
                }
                t.printAssertion(
                    "$batchSize annotations have been deleted",
                    annotationsDeleted == batchSize
                )

                val fc2 = getFieldInfo(containerName).getOrElse { throw RuntimeException() }
                t.printAssertion("fieldCounts should be empty", fc2.fieldInfo.isEmpty())
            }
        }

    private fun AnnoRepoClient.testContainerFieldCount(t: Terminal): Boolean =
        runTest(t, "Testing the annotation field counter") {
            inTemporaryContainer(this, t) { containerName ->
                val fc0 = getFieldInfo(containerName).getOrElse { throw RuntimeException() }
                t.printAssertion("Initially, fieldCounts should be empty", fc0.fieldInfo.isEmpty())

                val annotation: Map<String, Any> = mapOf("body" to mapOf("id" to "urn:example:blahblahblah"))
                t.printStep("Adding annotation with body.id field: ")
                t.printJson(annotation)
                val car = createAnnotation(containerName, annotation).getOrElse { throw Exception() }
//                t.println(green(car.toString()))

                val fc1 = getFieldInfo(containerName).getOrElse { throw Exception() }
//                t.println(green(fc1.toString()))
                t.printAssertion(
                    "fieldCounts should have body.id = 1",
                    fc1.fieldInfo.getOrDefault("body.id", 0) == 1
                )

                val newAnnotation: Map<String, Any> = mapOf("body" to "urn:example:blahblahblah")
                t.printStep("Updating the annotation: ")
                t.printJson(newAnnotation)
                val uar = updateAnnotation(
                    car.containerName,
                    car.annotationName,
                    car.eTag,
                    newAnnotation
                ).getOrElse { er -> t.printJson(er); throw RuntimeException() }

                val fc2 = getFieldInfo(containerName).getOrElse { throw Exception() }
                t.printAssertion(
                    "fieldCounts should not have a body.id field",
                    !fc2.fieldInfo.containsKey("body.id")
                )
                t.printAssertion(
                    "fieldCounts should have body = 1",
                    fc2.fieldInfo.getOrDefault("body", 0) == 1
                )

                t.printStep("Deleting the annotation")
                deleteAnnotation(containerName, uar.containerName, uar.eTag)

                val fc3 =
                    getFieldInfo(containerName).getOrElse { er -> t.printJson(er); throw RuntimeException(er.message) }

                t.printAssertion("fieldCounts should be empty", fc3.fieldInfo.isEmpty())
            }
        }

    private fun AnnoRepoClient.testAdminEndpoints(t: Terminal): Boolean =
        runTest(t, "Testing /admin endpoints") {
            var numberOfUsers = 0
            t.printStep("Getting the list of users")
            this.getUsers().thenAssertResult(t) { result ->
                val list = result.userEntries
                numberOfUsers = list.size
                t.println("$numberOfUsers users")
                t.printJson(list)
                true
            }

            val userName = "username1"
            t.printStep("Adding a user")
            this.addUsers(listOf(UserEntry(userName, "apiKey1"))).thenAssertResult(t) { result ->
                t.printJson(result.accepted)
                t.printJson(result.rejected)
                true
            }


            t.printStep("Getting the new list of users")
            this.getUsers().thenAssertResult(t) { result ->
                val list = result.userEntries
                t.printJson(list)
                val newNumberOfUsers = list.size
                t.printAssertion("Number of users should be 1 higher", (newNumberOfUsers == numberOfUsers + 1))
                true
            }

            t.printStep("Deleting a user")
            this.deleteUser(userName).thenAssertResult(t) { response ->
                t.printJson(response.response.status)
                true
            }

            t.printStep("Getting the list of users again")
            this.getUsers().thenAssertResult(t) { result ->
                val list = result.userEntries
                t.printJson(list)
                val newNumberOfUsers = list.size
                t.printAssertion("Number of users should be back to the original", (newNumberOfUsers == numberOfUsers))
                true
            }

        }

    private fun Terminal.printAssertion(message: String, assertion: Boolean) =
        println("- $message: ${assertion.asCheckMark()}")

    private fun Terminal.printStep(message: String) =
        println("> $message")

    private fun Terminal.printJson(obj: Any) =
        println(blue(jsonWriter.writeValueAsString(obj)))

    private fun Terminal.printError(obj: Any) =
        println(red(jsonWriter.writeValueAsString(obj)))

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
        func: (containerName: String) -> Unit?,
    ) {
        t.printStep("Creating container $containerName")
        val r = ac.createContainer(containerName).getOrElse { throw Exception() }
        try {
            func(r.containerName)
        } finally {
            t.printStep("Deleting container $containerName")
            val deleteResult = ac.deleteContainer(r.containerName, eTag = r.eTag)
            assert(deleteResult.isRight())
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
            val serverURL by parser.option(
                ArgType.String,
                shortName = "s",
                description = "URL of the AnnoRepo server to test."
            ).required()
            val apiKey by parser.option(
                ArgType.String,
                shortName = "k",
                description = "api-key of the user to test as."
            )
            parser.parse(args)
            IntegrationTest().testServer(serverURL, apiKey)
        }
    }
}

