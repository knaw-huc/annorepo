package nl.knaw.huc.annorepo.resources

import java.time.Instant
import java.util.Date
import jakarta.ws.rs.BadRequestException
import jakarta.ws.rs.core.SecurityContext
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.runs
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import nl.knaw.huc.annorepo.api.CustomQuerySpecs
import nl.knaw.huc.annorepo.auth.RootUser
import nl.knaw.huc.annorepo.config.AnnoRepoConfiguration
import nl.knaw.huc.annorepo.dao.ContainerDAO
import nl.knaw.huc.annorepo.dao.ContainerUserDAO
import nl.knaw.huc.annorepo.dao.CustomQuery
import nl.knaw.huc.annorepo.dao.CustomQueryDAO
import nl.knaw.huc.annorepo.resources.tools.SearchManager
import nl.knaw.huc.annorepo.service.UriFactory

@ExtendWith(MockKExtension::class)
class GlobalServiceResourceTest {

    @Test
    fun `test createCustomQuery with valid json`() {
//        val customQueryString = """
//            {
//              "name": "$CUSTOM_QUERY_NAME",
//              "label": "All the resolutions",
//              "query": {
//                "body.type":"Resolution"
//              }
//            }
//        """.trimIndent()
        val setting = CustomQuerySpecs(
            name = CUSTOM_QUERY_NAME,
            label = "All the resolutions",
            query = mapOf("body,type" to "Resolution"),
            description = "",
            public = true
        )
        val response = resource.createCustomQuery(setting, securityContext)
        println(response)
        println(response.location)
    }

    @Test
    fun `createCustomQuery with existing name throws exception`() {
//        val customQueryString = """
//            {
//              "name": "$CUSTOM_QUERY_NAME",
//              "query": {
//                "body.type":"Resolution"
//              }
//            }
//        """.trimIndent()
        val setting = CustomQuerySpecs(
            name = CUSTOM_QUERY_NAME,
            label = "All the lines",
            query = mapOf("body,type" to "Line"),
        )
        every { customQueryDAO.nameIsTaken(CUSTOM_QUERY_NAME) } returns true
        assertThatExceptionOfType(BadRequestException::class.java)
            .isThrownBy {
                val response = resource.createCustomQuery(setting, securityContext)
                println(response)
            }.withMessage(
                """A custom query with the name '$CUSTOM_QUERY_NAME' already exists"""
            )
    }

//    @Test
//    fun `test createCustomQuery with invalid json`() {
//        val customQueryString = "Hello World"
//        assertThatExceptionOfType(BadRequestException::class.java)
//            .isThrownBy {
//                val response = resource.createCustomQuery(customQueryString, securityContext)
//                println(response)
//            }.withMessage(
//                """invalid json: Unrecognized token 'Hello': was expecting (JSON String, Number, Array, Object or token 'null', 'true' or 'false')
// at [Source: REDACTED (`StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION` disabled); line: 1, column: 6]"""
//            )
//    }

    @Test
    fun `test getCustomQuery with valid name`() {
        val response = resource.getCustomQuery("all-resolutions", securityContext)
        println(response)
        println(response.location)
    }

    @Test
    fun `test custom query as json`() {
        val json = jacksonObjectMapper().writeValueAsString(allResolutionsCustomQuery)
        val expected = """
            {
                "name": "all-resolutions",
                "created": "2024-02-27T12:30:00+0000",
                "createdBy": "",
                "public": true,
                "queryTemplate": "{\"body.type\":\"Resolution\"}"
            }
            """
        JSONAssert.assertEquals(expected, json, JSONCompareMode.LENIENT)

    }

    @Test
    fun `test getCustomQueries`() {
        val response = resource.getCustomQueries(securityContext)
        println(response)
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
        lateinit var customQueryDAO: CustomQueryDAO

        @MockK
        lateinit var searchManager: SearchManager

        @MockK
        lateinit var securityContext: SecurityContext

        private lateinit var uriFactory: UriFactory
        lateinit var resource: GlobalServiceResource
        const val CUSTOM_QUERY_NAME = "all-resolutions"
        private val allResolutionsCustomQuery = CustomQuery(
            name = CUSTOM_QUERY_NAME,
            queryTemplate = """{"body.type":"Resolution"}""",
            created = Date.from(Instant.parse("2024-02-27T12:30:00Z"))
        )

        @BeforeAll
        @JvmStatic
        internal fun beforeAll() {
            MockKAnnotations.init(this)
            every { configuration.externalBaseUrl } returns BASE_URL
            every { configuration.withAuthentication } returns true

            every { securityContext.userPrincipal } returns RootUser()

            every { customQueryDAO.store(any()) } just runs
            every { customQueryDAO.nameIsTaken(any()) } returns false
            every { customQueryDAO.getByName(CUSTOM_QUERY_NAME) } returns allResolutionsCustomQuery
            every { customQueryDAO.getAllCustomQueries() } returns listOf(
                allResolutionsCustomQuery
            )

            uriFactory = UriFactory(configuration)
            resource = GlobalServiceResource(
                configuration = configuration,
                containerDAO = containerDAO,
                containerUserDAO = containerUserDAO,
                customQueryDAO = customQueryDAO,
                searchManager = searchManager,
                uriFactory = uriFactory
            )
        }
    }
}