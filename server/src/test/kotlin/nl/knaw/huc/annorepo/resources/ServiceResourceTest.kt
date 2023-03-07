package nl.knaw.huc.annorepo.resources

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
import nl.knaw.huc.annorepo.api.Role
import nl.knaw.huc.annorepo.auth.ContainerUserDAO
import nl.knaw.huc.annorepo.auth.RootUser
import nl.knaw.huc.annorepo.config.AnnoRepoConfiguration
import org.assertj.core.api.AssertionsForInterfaceTypes.assertThat
import org.bson.Document
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.slf4j.LoggerFactory
import java.net.URI
import java.security.Principal
import javax.ws.rs.NotAuthorizedException
import javax.ws.rs.core.SecurityContext

@ExtendWith(MockKExtension::class)
class ServiceResourceTest {

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

    @Test
    fun `readContainerUsers returns something when the user is authorized in the container`() {
        val userName = "adminUser"
        every { mockContainerUserDAO.getUserRole(containerName, userName) } returns Role.ADMIN
        every { userPrincipal.name } returns userName
        every { securityContext.userPrincipal } returns userPrincipal
        val response = resource.readContainerUsers(containerName, securityContext)
        log.info("response={}", response)
        assertThat(response.status).isEqualTo(200)
    }

    @Test
    fun `readContainerUsers returns something when the user is root`() {
        every { securityContext.userPrincipal } returns RootUser()
        val response = resource.readContainerUsers(containerName, securityContext)
        log.info("response={}", response)
        assertThat(response.status).isEqualTo(200)
    }

    @Test
    fun `readContainerUsers fails when the user is not admin`() {
        val userName = "root"
        every { mockContainerUserDAO.getUserRole(containerName, userName) } returns Role.GUEST
        every { userPrincipal.name } returns userName
        every { securityContext.userPrincipal } returns userPrincipal
        try {
            val response = resource.readContainerUsers(containerName, securityContext)
            log.info("response={}", response)
            fail()
        } catch (e: NotAuthorizedException) {
            assertThat(e.message).isEqualTo("HTTP 401 Unauthorized")
        }
    }

    @Test
    fun `readContainerUsers fails when the user is not connected to the container`() {
        val userName = "root"
        every { mockContainerUserDAO.getUserRole(containerName, userName) } returns null
        every { userPrincipal.name } returns userName
        every { securityContext.userPrincipal } returns userPrincipal
        try {
            val response = resource.readContainerUsers(containerName, securityContext)
            log.info("response={}", response)
            fail()
        } catch (e: NotAuthorizedException) {
            assertThat(e.message).isEqualTo("HTTP 401 Unauthorized")
        }
    }

    companion object {
        const val containerName = "containername"
        private const val baseURL = "https://annorepo.net"
        private const val databaseName = "mock"

        @MockK
        lateinit var config: AnnoRepoConfiguration

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
        lateinit var mockContainerUserDAO: ContainerUserDAO

        private lateinit var resource: ServiceResource
        private val log = LoggerFactory.getLogger(ServiceResourceTest::class.java)

        @BeforeAll
        @JvmStatic
        internal fun beforeAll() {
            MockKAnnotations.init(this)
            every { config.externalBaseUrl } returns baseURL
            every { config.databaseName } returns databaseName
            every { config.pageSize } returns 10
            every { config.rangeSelectorType } returns "something"
            every { client.getDatabase(databaseName) } returns mongoDatabase
            every { mongoDatabase.getCollection(containerName) } returns mongoCollection
            every { mongoDatabase.listCollectionNames() } returns collectionNames
            every { collectionNames.iterator() } returns mongoCursor
            every { mongoCursor.hasNext() } returns true
            every { mongoCursor.next() } returns containerName
            resource = ServiceResource(config, client, mockContainerUserDAO)
        }
    }
}