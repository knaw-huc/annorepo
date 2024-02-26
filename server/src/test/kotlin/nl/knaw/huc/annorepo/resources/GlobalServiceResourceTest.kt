package nl.knaw.huc.annorepo.resources

import jakarta.ws.rs.BadRequestException
import jakarta.ws.rs.core.SecurityContext
import kotlin.test.fail
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.assertj.core.api.Assertions.assertThat
import nl.knaw.huc.annorepo.auth.RootUser
import nl.knaw.huc.annorepo.config.AnnoRepoConfiguration
import nl.knaw.huc.annorepo.dao.ContainerDAO
import nl.knaw.huc.annorepo.dao.ContainerUserDAO
import nl.knaw.huc.annorepo.resources.tools.SearchManager
import nl.knaw.huc.annorepo.service.UriFactory

@ExtendWith(MockKExtension::class)
class GlobalServiceResourceTest {

    @Test
    fun `test createCustomQuery with valid json`() {
        val customQueryString = """
            {
              "name": "all-resolutions",
              "query": {
                "body.type":"Resolution"
              }
            }
        """.trimIndent()
        val response = resource.createCustomQuery(customQueryString, securityContext)
        println(response)
        println(response.location)
    }

    @Test
    fun `test createCustomQuery with invalid json`() {
        val customQueryString = "Hello World"
        try {
            val response = resource.createCustomQuery(customQueryString, securityContext)
            println(response)
            fail("expected a BadRequestException")
        } catch (e: BadRequestException) {
            assertThat(e).hasMessage(
                """invalid json: Unrecognized token 'Hello': was expecting (JSON String, Number, Array, Object or token 'null', 'true' or 'false')
 at [Source: REDACTED (`StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION` disabled); line: 1, column: 6]"""
            )
        }
    }

    companion object {

        private const val BASE_URL = "http://annorepo.ai/"

        @MockK
        lateinit var configuration: AnnoRepoConfiguration

        @MockK
        lateinit var containerDAO: ContainerDAO

        @MockK
        lateinit var containerUserDAO: ContainerUserDAO

        @MockK
        lateinit var searchManager: SearchManager

        @MockK
        lateinit var securityContext: SecurityContext

        lateinit var uriFactory: UriFactory
        lateinit var resource: GlobalServiceResource

        @BeforeAll
        @JvmStatic
        internal fun beforeAll() {
            MockKAnnotations.init(this)
            every { configuration.externalBaseUrl } returns BASE_URL
            every { configuration.withAuthentication } returns true
            every { securityContext.userPrincipal } returns RootUser()
            uriFactory = UriFactory(configuration)
            resource = GlobalServiceResource(configuration, containerDAO, containerUserDAO, searchManager, uriFactory)
        }
    }
}