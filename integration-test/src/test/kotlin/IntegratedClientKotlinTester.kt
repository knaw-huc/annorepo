import arrow.core.Either
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import nl.knaw.huc.annorepo.api.UserEntry
import nl.knaw.huc.annorepo.client.ARResult.AddUsersResult
import nl.knaw.huc.annorepo.client.ARResult.AnnotationFieldInfoResult
import nl.knaw.huc.annorepo.client.ARResult.UsersResult
import nl.knaw.huc.annorepo.client.AnnoRepoClient
import nl.knaw.huc.annorepo.client.AnnoRepoClient.Companion.create
import nl.knaw.huc.annorepo.client.FilterContainerAnnotationsResult
import nl.knaw.huc.annorepo.client.RequestError
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.net.URI
import java.util.stream.Stream

class IntegratedClientKotlinTester {
    //    Intentionally not named ClientTest, so mvn test will skip these integration tests

    @Nested
    inner class ConstructorTests {
        @Test
        fun testClientConstructor1() {
            val client = AnnoRepoClient(BASE_URI)
            assertThat(client.serverVersion).isNotBlank
        }

        @Test
        fun testClientConstructor2() {
            val client = AnnoRepoClient(BASE_URI, apiKey)
            assertThat(client.serverVersion).isNotBlank
        }

        @Test
        fun testClientConstructor3() {
            val client = AnnoRepoClient(
                BASE_URI,
                apiKey,
                "custom-user-agent"
            )
            assertThat(client.serverVersion).isNotBlank
        }

        @Test
        fun testCreate1() {
            val client = create(BASE_URI)
            assertClient(client)
        }

        @Test
        fun testCreate1a() {
            val client = create(BASE_URL)
            assertClient(client)
        }

        @Test
        fun testCreate2() {
            val client = create(BASE_URI, "root")
            assertClient(client)
        }

        @Test
        fun testCreate2a() {
            val client = create(BASE_URL, "root")
            assertClient(client)
        }

        @Test
        fun testCreate3() {
            val client = create(BASE_URI, "root", "custom-user-agent")
            assertClient(client)
        }

        @Test
        fun testCreate3a() {
            val client = create(BASE_URL, "root", "custom-user-agent")
            assertClient(client)
        }

        private fun assertClient(client: AnnoRepoClient?) {
            assertThat(client).isNotNull
            assertThat(client!!.serverVersion).isNotBlank
        }

        @Test
        fun testFailingCreate() {
            val client = create(URI.create("http://nothingtoseehere"))
            assertThat(client).isNull()
        }
    }

    @Nested
    inner class SearchTests {
        @Test
        fun testFilterContainerAnnotations() {
            val containerName = "volume-1728"
            val query = mapOf("body.type" to "Page")
            client.filterContainerAnnotations(containerName, query).fold(
                { error: RequestError -> handleError(error) },
                { (searchId, annotations): FilterContainerAnnotationsResult ->
                    annotations.forEach { a: Either<RequestError, String> ->
                        a.fold(
                            { e: RequestError -> handleError(e) },
                            { jsonString: String -> doSomethingWith(searchId, jsonString) }
                        )
                    }
                }
            )
        }

        @Test
        fun testFilterContainerAnnotations2() {
            val containerName = "volume-1728"
            val query = mapOf("body.type" to "Page")

            val e: Either<RequestError, List<String>> =
                client.filterContainerAnnotations2(containerName, query).flatten()
            when (e) {
                is Either.Left -> println(e.value)
                is Either.Right -> println(e.value.size)
            }
        }

        private fun Stream<Either<RequestError, String>>.flatten(): Either<RequestError, List<String>> {
            val list: MutableList<String> = mutableListOf()
            var error: RequestError? = null
            forEach { e ->
                when (e) {
                    is Either.Left -> error = e.value
                    is Either.Right -> list.add(e.value)
                }
            }
            return if (error == null) {
                Either.Right(list.toList())
            } else {
                Either.Left(error!!)
            }
        }
    }

    @Nested
    inner class FieldInfoTests {
        @Test
        fun testFieldInfo() {
            val containerName = "volume-1728"
            client.getFieldInfo(containerName).fold(
                { error: RequestError ->
                    handleError(error)
                },
                { (_, fieldInfo): AnnotationFieldInfoResult ->
                    doSomethingWith(fieldInfo)
                }
            )
        }
    }

    @Nested
    inner class AdminTests {
        @Test
        fun testUserCreateAndDelete() {
            val userName = "userName"
            val userEntries = listOf(UserEntry(userName, "apiKey"))
            client.addUsers(userEntries).fold(
                { error: RequestError ->
                    handleError(error)
                    false
                },
                { (_, accepted, rejected): AddUsersResult ->
                    doSomethingWith(accepted, rejected)
                    true
                }
            )
            val deletionSuccess: Boolean = client.deleteUser(userName).isRight()
            assertThat(deletionSuccess).isTrue
        }

        @Test
        fun testGetUsers() {
            client.getUsers().fold(
                { error: RequestError ->
                    handleError(error)
                    false
                },
                { (_, userEntries): UsersResult ->
                    doSomethingWith(userEntries)
                    true
                }
            )
        }
    }

    @Test
    fun testAbout() {
        val getAboutResult = client.getAbout().orNull()
        val aboutInfo = getAboutResult!!.aboutInfo
        doSomethingWith(aboutInfo)
        assertThat(aboutInfo).isNotNull
        assertThat(aboutInfo.appName).isEqualTo("AnnoRepo")
    }

    companion object {
        const val BASE_URL = "http://localhost:8080"
        val BASE_URI: URI = URI.create(BASE_URL)
        private const val apiKey = "root"
        val client = AnnoRepoClient(BASE_URI, apiKey, "integrated-client-tester")
        private val log = LoggerFactory.getLogger(IntegratedClientKotlinTester::class.java)

        private fun handleError(error: RequestError) {
            println(error.message)
        }

        private fun doSomethingWith(vararg objects: Any) {
            for (o in objects) {
                try {
                    log.info("{}", ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(o))
                } catch (e: JsonProcessingException) {
                    e.printStackTrace()
                }
            }
        }
    }

}