package nl.knaw.huc.annorepo.resources

import jakarta.ws.rs.core.Response
import jakarta.ws.rs.core.SecurityContext
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import com.mongodb.kotlin.client.ListCollectionNamesIterable
import com.mongodb.kotlin.client.MongoClient
import com.mongodb.kotlin.client.MongoCollection
import com.mongodb.kotlin.client.MongoCursor
import com.mongodb.kotlin.client.MongoDatabase
import org.assertj.core.api.Assertions.assertThat
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import nl.knaw.huc.annorepo.api.ContainerMetadata
import nl.knaw.huc.annorepo.api.ContainerSpecs
import nl.knaw.huc.annorepo.config.AnnoRepoConfiguration
import nl.knaw.huc.annorepo.dao.ContainerDAO
import nl.knaw.huc.annorepo.dao.ContainerUserDAO
import nl.knaw.huc.annorepo.resources.tools.IndexManager
import nl.knaw.huc.annorepo.service.UriFactory

class W3CResourceTest {

    private val mongoCursor: MongoCursor<String> = mock()
    private val mongoIterable: ListCollectionNamesIterable = mock {
        on { cursor() }.doReturn(mongoCursor)
    }
    private val collection: MongoCollection<ContainerMetadata> = mock()
    private val mdb: MongoDatabase = mock {
        on { listCollectionNames() }.doReturn(mongoIterable)
        on { getCollection(anyString(), eq(ContainerMetadata::class.java)) }.doReturn(collection)
    }
    private val client: MongoClient = mock {
        on { getDatabase(anyString()) }.doReturn(mdb)
    }
    private val configuration: AnnoRepoConfiguration = mock {
        on { databaseName }.doReturn("annorepo")
    }
    private val containerDAO: ContainerDAO = mock()
    private val containerUserDAO: ContainerUserDAO = mock()
    private val securityContext: SecurityContext = mock()
    private val indexManager: IndexManager = mock()

    @Disabled
    @Test
    fun `create container works as expected`() {
        configuration.databaseName = "annorepo"
        println(configuration.databaseName)
        println(client.getDatabase(configuration.databaseName))
        val r =
            W3CResource(
                configuration = configuration,
                containerDAO = containerDAO,
                containerUserDAO = containerUserDAO,
                uriFactory = UriFactory(configuration),
                indexManager = indexManager
            )
        val response = r.createContainer(
            containerSpecs = ContainerSpecs(
                context = mutableListOf(),
                type = listOf(),
                label = "container label",
                readOnlyForAnonymousUsers = true
            ), slug = "container",
            context = securityContext
        )
        println(response)
        assertThat(response.status).isEqualTo(Response.Status.CREATED.statusCode)
        assertThat(response.headers).containsAllEntriesOf(
            mapOf(
                "Accept-Post" to listOf("""application/ld+json; profile="http://www.w3.org/ns/anno.jsonld", text/turtle""")
            )
        )
    }

    @Test
    fun `last page calculation is correct`() {
        assertThat(lastPage(count = 100, pageSize = 99)).isEqualTo(1)
        assertThat(lastPage(count = 100, pageSize = 100)).isEqualTo(0)
        assertThat(lastPage(count = 100, pageSize = 101)).isEqualTo(0)
        assertThat(lastPage(count = 0, pageSize = 99)).isEqualTo(0)
        assertThat(lastPage(count = 100, pageSize = 50)).isEqualTo(1)
        assertThat(lastPage(count = 100, pageSize = 40)).isEqualTo(2)
    }

    private fun lastPage(count: Long, pageSize: Int) = (count - 1).div(pageSize)

}

