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
import com.mongodb.kotlin.client.ListCollectionNamesIterable
import com.mongodb.kotlin.client.MongoClient
import com.mongodb.kotlin.client.MongoCollection
import com.mongodb.kotlin.client.MongoCursor
import com.mongodb.kotlin.client.MongoDatabase
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import org.apache.logging.log4j.kotlin.logger
import org.assertj.core.api.AssertionsForInterfaceTypes.assertThat
import org.assertj.core.api.AssertionsForInterfaceTypes.assertThatExceptionOfType
import org.bson.Document
import nl.knaw.huc.annorepo.api.ARConst
import nl.knaw.huc.annorepo.api.ContainerMetadata
import nl.knaw.huc.annorepo.api.ContainerUserEntry
import nl.knaw.huc.annorepo.api.Role
import nl.knaw.huc.annorepo.auth.RootUser
import nl.knaw.huc.annorepo.config.AnnoRepoConfiguration
import nl.knaw.huc.annorepo.dao.ContainerDAO
import nl.knaw.huc.annorepo.dao.ContainerUserDAO
import nl.knaw.huc.annorepo.dao.CustomQueryDAO
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
                    val response = resource.readContainerUsers(CONTAINER_NAME, securityContext)
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
                    val response = resource.addContainerUsers(CONTAINER_NAME, securityContext, containerUsers)
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
                    val response = resource.deleteContainerUser(CONTAINER_NAME, "username", securityContext)
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
                    val response = resource.createSearch(CONTAINER_NAME, queryJson, context = securityContext)
                    logger.info { "response=$response" }
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
                val response = resource.createSearch(CONTAINER_NAME, queryJson, context = securityContext)
                logger.info { "result=$response" }
                val locations = response.headers["location"] as List<*>
                val location: URI = locations[0] as URI

                logger.info { "location=$location" }
                assertThat(location).hasHost("annorepo.net")
                assertThat(location.toString())
                    .startsWith("https://annorepo.net/services/containername/search/")
                val searchId = location.path.split('/').last()
                logger.info { "searchId=$searchId" }

                val searchResponse = resource.getSearchResultPage(
                    CONTAINER_NAME,
                    searchId,
                    userAgent = "AnnoRepoClient 0.7",
                    context = securityContext
                )
                logger.info { "searchResponse=$searchResponse" }
                logger.info { "searchResponse.entity=${searchResponse.entity}" }
            }
        }

        @Nested
        inner class GetSearchResultPageTest {
            @Test
            fun `getSearchResultPage endpoint can be used by root, admin, editor and guest, but not by others`() {
                assertRoleAuthorizationForBlock(
                    authorizedRoles = setOf(Role.ROOT, Role.ADMIN, Role.EDITOR, Role.GUEST)
                ) {
                    val response = resource.getSearchResultPage(
                        CONTAINER_NAME,
                        "some-search-id",
                        0,
                        userAgent = "AnnoRepoClient 0.6",
                        securityContext
                    )
                    logger.info { "searchResponse.entity=${response.entity}" }
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
                    val response = resource.getSearchInfo(CONTAINER_NAME, SEARCH_ID, securityContext)
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
                    val response = resource.getAnnotationFieldsForContainer(CONTAINER_NAME, securityContext)
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
                        resource.getDistinctAnnotationFieldsValuesForContainer(CONTAINER_NAME, "type", securityContext)
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
                    val response = resource.getMetadataForContainer(CONTAINER_NAME, securityContext)
                    assertNotNull(response)
                }
            }
        }

        @Nested
        inner class SetAnonymousReadAccessForContainerTest {
            @Test
            fun `setAnonymousUserReadAccess endpoint can be used by root and admin, but not by others`() {
                assertRoleAuthorizationForBlock(
                    authorizedRoles = setOf(Role.ROOT, Role.ADMIN)
                ) {
                    val response = resource.setAnonymousUserReadAccess(CONTAINER_NAME, true, securityContext)
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
                    val response = resource.getContainerIndexes(CONTAINER_NAME, securityContext)
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
                    val response =
                        resource.addContainerIndex(CONTAINER_NAME, mapOf("fieldName" to "indexType"), securityContext)
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
                        resource.getContainerIndexDefinition(
                            CONTAINER_NAME,
                            "indexId",
                            securityContext
                        )
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
                        resource.deleteContainerIndex(
                            CONTAINER_NAME,
                            "indexId",
                            securityContext
                        )
                    assertNotNull(response)
                }
            }
        }

        @Nested
        inner class AddMultiFieldContainerIndexTest {
            @Test
            fun `addMultiFieldContainerIndex endpoint can be used by root or admin, but not by others`() {
                assertRoleAuthorizationForBlock(
                    authorizedRoles = setOf(Role.ROOT, Role.ADMIN)
                ) {
                    val response =
                        resource.addContainerIndex(CONTAINER_NAME, mapOf(), securityContext)
                    assertNotNull(response)
                }
            }

        }
    }

    companion object {
        const val CONTAINER_NAME = "containername"
        const val SEARCH_ID = "some-search-id"
        private const val BASE_URL = "https://annorepo.net"
        private const val DATABASE_NAME = "mock"

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
        lateinit var collectionNames: ListCollectionNamesIterable

        @RelaxedMockK
        lateinit var mongoCursor: MongoCursor<String>

        @RelaxedMockK
        lateinit var containerUserDAO: ContainerUserDAO

        @RelaxedMockK
        lateinit var customQueryDAO: CustomQueryDAO

        @RelaxedMockK
        lateinit var containerDAO: ContainerDAO

        private lateinit var resource: ContainerServiceResource

        @BeforeAll
        @JvmStatic
        internal fun beforeAll() {
            MockKAnnotations.init(this)
            every { containerDAO.containerExists(CONTAINER_NAME) } returns true
            every { config.externalBaseUrl } returns BASE_URL
            every { config.databaseName } returns DATABASE_NAME
            every { config.pageSize } returns 10
            every { config.rangeSelectorType } returns "something"
            every { config.withAuthentication } returns true
            every { client.getDatabase(DATABASE_NAME) } returns mongoDatabase
            every { mongoDatabase.getCollection<Document>(CONTAINER_NAME) } returns mongoCollection
            every { mongoDatabase.listCollectionNames() } returns collectionNames
            every { collectionNames.cursor() } returns mongoCursor
            every { mongoCursor.hasNext() } returns true
            every { mongoCursor.next() } returns CONTAINER_NAME
            every { containerDAO.getContainerMetadata(ARConst.CONTAINER_METADATA_COLLECTION) } returns ContainerMetadata(
                name = "name",
                label = "label",
                isReadOnlyForAnonymous = false,
                indexMap = mutableMapOf("index-0" to "annotation.body.id_1")
            )
            resource = ContainerServiceResource(
                config,
                containerUserDAO,
                containerDAO,
                customQueryDAO,
                UriFactory(config),
                indexManager,
            )
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
            every { containerUserDAO.getUserRole(CONTAINER_NAME, userName) } returns role
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
                    logger.info { e.stackTraceToString() }
                }
            }
            for (role in unauthorizedRoles) {
                if (role == Role.ROOT) {
                    useRootUser()
                } else {
                    useUserWithRole("unauthorized_user", role)
                }
                assertThatExceptionOfType(NotAuthorizedException::class.java)
                    .isThrownBy { block() }
                    .withMessage("HTTP 401 Unauthorized")

            }
        }
    }
}
