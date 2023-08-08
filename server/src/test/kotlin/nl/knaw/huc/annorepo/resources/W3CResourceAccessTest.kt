package nl.knaw.huc.annorepo.resources

import java.security.Principal
import jakarta.ws.rs.NotAuthorizedException
import jakarta.ws.rs.core.Request
import jakarta.ws.rs.core.SecurityContext
import kotlin.test.assertNotNull
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoCursor
import com.mongodb.client.MongoDatabase
import com.mongodb.client.MongoIterable
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import org.assertj.core.api.AssertionsForInterfaceTypes.assertThat
import org.bson.Document
import org.slf4j.LoggerFactory
import nl.knaw.huc.annorepo.api.ContainerMetadata
import nl.knaw.huc.annorepo.api.Role
import nl.knaw.huc.annorepo.auth.RootUser
import nl.knaw.huc.annorepo.config.AnnoRepoConfiguration
import nl.knaw.huc.annorepo.dao.ContainerUserDAO
import nl.knaw.huc.annorepo.resources.tools.IndexManager
import nl.knaw.huc.annorepo.service.UriFactory

@ExtendWith(MockKExtension::class)
class W3CResourceAccessTest {
    @Nested
    inner class ContainerTest {

        @Test
        fun `readContainer endpoint can be used by root, admin, editor and guest, but not by others`() {
            assertRoleAuthorizationForBlock(
                authorizedRoles = setOf(Role.ROOT, Role.ADMIN, Role.EDITOR, Role.GUEST)
            ) {
                val response =
                    resource.readContainer(containerName = CONTAINER_NAME, page = 0, context = securityContext)
                assertNotNull(response)
            }
        }

        @Test
        fun `deleteContainer endpoint can be used by root and admin, but not by others`() {
            assertRoleAuthorizationForBlock(
                authorizedRoles = setOf(Role.ROOT, Role.ADMIN)
            ) {
                val response =
                    resource.deleteContainer(
                        containerName = CONTAINER_NAME,
                        req = request,
                        context = securityContext
                    )
                assertNotNull(response)
            }
        }
    }

    @Nested
    inner class AnnotationTest {

        @Test
        fun `createAnnotation endpoint can be used by root, admin and editor, but not by others`() {
            assertRoleAuthorizationForBlock(
                authorizedRoles = setOf(Role.ROOT, Role.ADMIN, Role.EDITOR)
            ) {
                val response =
                    resource.createAnnotation(
                        slug = "slug",
                        containerName = CONTAINER_NAME,
                        annotationJson = "",
                        context = securityContext
                    )
                assertNotNull(response)
            }
        }

        @Test
        fun `readAnnotation endpoint can be used by root, admin, editor and guest, but not by others`() {
            assertRoleAuthorizationForBlock(
                authorizedRoles = setOf(Role.ROOT, Role.ADMIN, Role.EDITOR, Role.GUEST)
            ) {
                val response =
                    resource.readAnnotation(
                        containerName = CONTAINER_NAME,
                        annotationName = "annotation",
                        context = securityContext
                    )
                assertNotNull(response)
            }
        }

        @Test
        fun `updateAnnotation endpoint can be used by root, admin and editor, but not by others`() {
            assertRoleAuthorizationForBlock(
                authorizedRoles = setOf(Role.ROOT, Role.ADMIN, Role.EDITOR)
            ) {
                val response =
                    resource.updateAnnotation(
                        containerName = CONTAINER_NAME,
                        annotationName = "annotation",
                        annotationJson = "",
                        req = request,
                        context = securityContext
                    )
                assertNotNull(response)
            }
        }

        @Test
        fun `deleteAnnotation endpoint can be used by root, admin and editor, but not by others`() {
            assertRoleAuthorizationForBlock(
                authorizedRoles = setOf(Role.ROOT, Role.ADMIN, Role.EDITOR)
            ) {
                val response =
                    resource.deleteAnnotation(
                        containerName = CONTAINER_NAME,
                        annotationName = "annotation",
                        req = request,
                        context = securityContext
                    )
                assertNotNull(response)
            }
        }
    }

    companion object {
        private const val CONTAINER_NAME = "container-name"
        private const val BASE_URL = "https://annorepo.net"
        private const val DATABASE_NAME = "mock"

        @MockK
        lateinit var config: AnnoRepoConfiguration

        @MockK
        lateinit var securityContext: SecurityContext

        @MockK
        lateinit var userPrincipal: Principal

        @MockK
        lateinit var client: MongoClient

        @MockK
        lateinit var request: Request

        @MockK
        lateinit var mongoDatabase: MongoDatabase

        @RelaxedMockK
        lateinit var mongoCollection: MongoCollection<Document>

        @RelaxedMockK
        lateinit var containerMetadataCollection: MongoCollection<ContainerMetadata>

        @MockK
        lateinit var collectionNames: MongoIterable<String>

        @RelaxedMockK
        lateinit var mongoCursor: MongoCursor<String>

        @RelaxedMockK
        lateinit var containerUserDAO: ContainerUserDAO

        @RelaxedMockK
        lateinit var indexManager: IndexManager

        private lateinit var resource: W3CResource
        private val log = LoggerFactory.getLogger(W3CResourceAccessTest::class.java)

        @BeforeAll
        @JvmStatic
        internal fun beforeAll() {
            MockKAnnotations.init(this)
            every { config.externalBaseUrl } returns BASE_URL
            every { config.databaseName } returns DATABASE_NAME
            every { config.pageSize } returns 10
            every { config.rangeSelectorType } returns "something"
            every { config.withAuthentication } returns true
            every { client.getDatabase(DATABASE_NAME) } returns mongoDatabase
            every { mongoDatabase.getCollection(CONTAINER_NAME) } returns mongoCollection
            every {
                mongoDatabase.getCollection(
                    "_containerMetadata",
                    ContainerMetadata::class.java
                )
            } returns containerMetadataCollection
            every { mongoDatabase.listCollectionNames() } returns collectionNames
            every { mongoDatabase.createCollection("slug") } returns Unit
            every { collectionNames.iterator() } returns mongoCursor
            every { mongoCursor.hasNext() } returns true
            every { mongoCursor.next() } returns CONTAINER_NAME
            resource = W3CResource(
                configuration = config,
                client = client,
                containerUserDAO = containerUserDAO,
                uriFactory = UriFactory(config),
                indexManager = indexManager
            )
        }

        private fun useRootUser() {
            every { securityContext.userPrincipal } returns RootUser()
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
