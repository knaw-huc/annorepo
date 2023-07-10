package nl.knaw.huc.annorepo.resources

import java.net.URI
import java.security.Principal
import jakarta.ws.rs.NotAuthorizedException
import jakarta.ws.rs.core.SecurityContext
import kotlin.test.assertNotNull
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import com.mongodb.client.*
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import org.assertj.core.api.AssertionsForInterfaceTypes.assertThat
import org.bson.Document
import org.slf4j.LoggerFactory
import nl.knaw.huc.annorepo.api.ContainerUserEntry
import nl.knaw.huc.annorepo.api.Role
import nl.knaw.huc.annorepo.auth.RootUser
import nl.knaw.huc.annorepo.config.AnnoRepoConfiguration
import nl.knaw.huc.annorepo.dao.ContainerUserDAO
import nl.knaw.huc.annorepo.resources.tools.IndexManager
import nl.knaw.huc.annorepo.service.UriFactory

@ExtendWith(MockKExtension::class)
class ContainerServiceResourceTest {
    @Nested
    inner class ContainerUserTest {
        @Nested
        inner class ReadContainerUsersTest {
            @Test
            fun `readContainerUsers endpoint can be used by root and admin, but not by others`() {
                assertRoleAuthorizationForBlock(
                    authorizedRoles = setOf(Role.ROOT, Role.ADMIN)
                ) {
                    val response = resource.readContainerUsers(containerName, securityContext)
                    assertNotNull(response)
                }
            }
        }

        @Nested
        inner class AddContainerUsersTest {
            @Test
            fun `addContainerUsers endpoint can be used by root and admin, but not by others`() {
                assertRoleAuthorizationForBlock(
                    authorizedRoles = setOf(Role.ROOT, Role.ADMIN)
                ) {
                    val containerUsers = listOf(ContainerUserEntry("guestUser", Role.GUEST))
                    val response = resource.addContainerUsers(containerName, securityContext, containerUsers)
                    assertNotNull(response)
                }
            }
        }

        @Nested
        inner class DeleteContainerUserTest {
            @Test
            fun `deleteContainerUser endpoint can be used by root and admin, but not by others`() {
                assertRoleAuthorizationForBlock(
                    authorizedRoles = setOf(Role.ROOT, Role.ADMIN)
                ) {
                    val response = resource.deleteContainerUser(containerName, "username", securityContext)
                    assertNotNull(response)
                }
            }
        }
    }

    @Nested
    inner class ContainerSearchTest {

        @Nested
        inner class CreateSearchTest {
            @Test
            fun `createSearch endpoint can be used by root, admin, editor and guest users, but not by others`() {
                assertRoleAuthorizationForBlock(
                    authorizedRoles = setOf(Role.ROOT, Role.ADMIN, Role.EDITOR, Role.GUEST)
                ) {
                    val queryJson = """{ "body.id" : "something" }"""
                    val response = resource.createSearch(containerName, queryJson, context = securityContext)
                    log.info("response={}", response)
                }
            }

            @Test
            fun `createSearch should return a response with location`() {
                val queryJson = """
                {
                    ":overlapsWithTextAnchorRange": {
                        "source": "http://adsdasd",
                        "start": 12,
                        "end": 134
                    },
                    
                    "body.type": {
                        ":isNotIn": [
                            "Line",
                            "Page"
                        ]
                    }
                }
        """.trimIndent()
                useEditorUser()
                val response = resource.createSearch(containerName, queryJson, context = securityContext)
                log.info("result={}", response)
                val locations = response.headers["location"] as List<*>
                val location: URI = locations[0] as URI

                log.info("location={}", location)
                assertThat(location).hasHost("annorepo.net")
                assertThat(location.toString())
                    .startsWith("https://annorepo.net/services/containername/search/")
                val searchId = location.path.split('/').last()
                log.info("searchId={}", searchId)

                val searchResponse = resource.getSearchResultPage(containerName, searchId, context = securityContext)
                log.info("searchResponse={}", searchResponse)
                log.info("searchResponse.entity={}", searchResponse.entity)
            }
        }

        @Nested
        inner class GetSearchResultPageTest {
            @Test
            fun `getSearchResultPage endpoint can be used by root, admin, editor and guest, but not by others`() {
                assertRoleAuthorizationForBlock(
                    authorizedRoles = setOf(Role.ROOT, Role.ADMIN, Role.EDITOR, Role.GUEST)
                ) {
                    val response = resource.getSearchResultPage(containerName, "some-search-id", 0, securityContext)
                    assertNotNull(response)
                }
            }
        }

        @Nested
        inner class GetSearchInfoTest {
            @Test
            fun `getSearchInfo endpoint can be used by root, admin, editor and guest, but not by others`() {
                assertRoleAuthorizationForBlock(
                    authorizedRoles = setOf(Role.ROOT, Role.ADMIN, Role.EDITOR, Role.GUEST)
                ) {
                    val response = resource.getSearchInfo(containerName, searchId, securityContext)
                    assertNotNull(response)
                }
            }
        }
    }

    @Nested
    inner class ContainerMetadataTest {

        @Nested
        inner class GetAnnotationFieldsForContainerTest {
            @Test
            fun `getAnnotationFieldsForContainer endpoint can be used by root, admin, editor and guest, but not by others`() {
                assertRoleAuthorizationForBlock(
                    authorizedRoles = setOf(Role.ROOT, Role.ADMIN, Role.EDITOR, Role.GUEST)
                ) {
                    val response = resource.getAnnotationFieldsForContainer(containerName, securityContext)
                    assertNotNull(response)
                }
            }
        }

        @Nested
        inner class GetDistinctAnnotationFieldValuesForContainerTest {
            @Test
            fun `getDistinctAnnotationFieldsValuesForContainer endpoint can be used by root, admin, editor and guest, but not by others`() {
                assertRoleAuthorizationForBlock(
                    authorizedRoles = setOf(Role.ROOT, Role.ADMIN, Role.EDITOR, Role.GUEST)
                ) {
                    val response =
                        resource.getDistinctAnnotationFieldsValuesForContainer(containerName, "type", securityContext)
                    assertNotNull(response)
                }
            }
        }

        @Nested
        inner class GetMetadataForContainerTest {
            @Test
            fun `getMetadataForContainer endpoint can be used by root, admin, editor and guest, but not by others`() {
                assertRoleAuthorizationForBlock(
                    authorizedRoles = setOf(Role.ROOT, Role.ADMIN, Role.EDITOR, Role.GUEST)
                ) {
                    val response = resource.getMetadataForContainer(containerName, securityContext)
                    assertNotNull(response)
                }
            }
        }
    }

    @Nested
    inner class ContainerIndexTest {

        @Nested
        inner class GetContainerIndexesTest {
            @Test
            fun `getContainerIndexes endpoint can be used by root, admin, editor and guest, but not by others`() {
                assertRoleAuthorizationForBlock(
                    authorizedRoles = setOf(Role.ROOT, Role.ADMIN, Role.EDITOR, Role.GUEST)
                ) {
                    val response = resource.getContainerIndexes(containerName, securityContext)
                    assertNotNull(response)
                }
            }
        }

        @Nested
        inner class AddContainerIndexTest {
            @Test
            fun `addContainerIndex endpoint can be used by root or admin, but not by others`() {
                assertRoleAuthorizationForBlock(
                    authorizedRoles = setOf(Role.ROOT, Role.ADMIN)
                ) {
                    val response = resource.addContainerIndex(containerName, "fieldName", "indexType", securityContext)
                    assertNotNull(response)
                }
            }
        }

        @Nested
        inner class GetContainerIndexDefinitionTest {
            @Test
            fun `getContainerIndexDefinition endpoint can be used by root or admin, but not by others`() {
                assertRoleAuthorizationForBlock(
                    authorizedRoles = setOf(Role.ROOT, Role.ADMIN)
                ) {
                    val response =
                        resource.getContainerIndexDefinition(containerName, "fieldName", "indexType", securityContext)
                    assertNotNull(response)
                }
            }
        }

        @Nested
        inner class DeleteContainerIndexTest {
            @Test
            fun `deleteContainerIndex endpoint can be used by root or admin, but not by others`() {
                assertRoleAuthorizationForBlock(
                    authorizedRoles = setOf(Role.ROOT, Role.ADMIN)
                ) {
                    val response =
                        resource.deleteContainerIndex(containerName, "fieldName", "indexType", securityContext)
                    assertNotNull(response)
                }
            }
        }
    }

    companion object {
        const val containerName = "containername"
        const val searchId = "some-search-id"
        private const val baseURL = "https://annorepo.net"
        private const val databaseName = "mock"

        @MockK
        lateinit var config: AnnoRepoConfiguration

        @MockK
        lateinit var indexManager: IndexManager

        @MockK
        lateinit var securityContext: SecurityContext

        @MockK
        lateinit var userPrincipal: Principal

        @MockK
        lateinit var client: MongoClient

        @MockK
        lateinit var mongoDatabase: MongoDatabase

        @RelaxedMockK
        lateinit var mongoCollection: MongoCollection<Document>

        @MockK
        lateinit var collectionNames: MongoIterable<String>

        @RelaxedMockK
        lateinit var mongoCursor: MongoCursor<String>

        @RelaxedMockK
        lateinit var containerUserDAO: ContainerUserDAO

        private lateinit var resource: ContainerServiceResource
        private val log = LoggerFactory.getLogger(ContainerServiceResourceTest::class.java)

        @BeforeAll
        @JvmStatic
        internal fun beforeAll() {
            MockKAnnotations.init(this)
            every { config.externalBaseUrl } returns baseURL
            every { config.databaseName } returns databaseName
            every { config.pageSize } returns 10
            every { config.rangeSelectorType } returns "something"
            every { config.withAuthentication } returns true
            every { client.getDatabase(databaseName) } returns mongoDatabase
            every { mongoDatabase.getCollection(containerName) } returns mongoCollection
            every { mongoDatabase.listCollectionNames() } returns collectionNames
            every { collectionNames.iterator() } returns mongoCursor
            every { mongoCursor.hasNext() } returns true
            every { mongoCursor.next() } returns containerName
            resource = ContainerServiceResource(config, client, containerUserDAO, UriFactory(config), indexManager)
        }

        private fun useRootUser() {
            every { securityContext.userPrincipal } returns RootUser()
        }

        private fun useAdminUser() {
            useUserWithRole("admin", Role.ADMIN)
        }

        private fun useEditorUser() {
            useUserWithRole("editor", Role.EDITOR)
        }

        private fun useGuestUser() {
            useUserWithRole("guest", Role.GUEST)
        }

        private fun useUserWithoutContainerAccess() {
            useUserWithRole("anonymous", null)
        }

        private fun useUserWithRole(userName: String, role: Role?) {
            every { containerUserDAO.getUserRole(containerName, userName) } returns role
            every { userPrincipal.name } returns userName
            every { securityContext.userPrincipal } returns userPrincipal
        }

        private fun assertRoleAuthorizationForBlock(
            authorizedRoles: Set<Role?>,
            block: () -> Unit,
        ) {
            val allRoles = setOf(Role.ROOT, Role.ADMIN, Role.EDITOR, Role.GUEST, null)
            val unauthorizedRoles = allRoles - authorizedRoles

            for (role in authorizedRoles) {
                if (role == Role.ROOT) {
                    useRootUser()
                } else {
                    useUserWithRole("authorized_user", role)
                }
                try {
                    block()
                } catch (e: NotAuthorizedException) {
                    fail("User with role $role should have been authorized!")
                } catch (e: RuntimeException) {
                    log.info(e.stackTraceToString())
                }
            }
            for (role in unauthorizedRoles) {
                if (role == Role.ROOT) {
                    useRootUser()
                } else {
                    useUserWithRole("unauthorized_user", role)
                }
                try {
                    block()
                    fail("User with role $role is unexpectedly authorized!")
                } catch (e: NotAuthorizedException) {
                    assertThat(e.message).isEqualTo("HTTP 401 Unauthorized")
                }
            }
        }
    }
}
