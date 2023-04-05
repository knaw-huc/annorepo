package nl.knaw.huc.annorepo.resources

import javax.ws.rs.core.Response
import javax.ws.rs.core.SecurityContext
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoCursor
import com.mongodb.client.MongoDatabase
import com.mongodb.client.MongoIterable
import org.assertj.core.api.Assertions.assertThat
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import nl.knaw.huc.annorepo.api.ContainerMetadata
import nl.knaw.huc.annorepo.api.ContainerSpecs
import nl.knaw.huc.annorepo.auth.ContainerUserDAO
import nl.knaw.huc.annorepo.config.AnnoRepoConfiguration

class W3CResourceTest {

    private val mongoCursor: MongoCursor<String> = mock()
    private val mongoIterable: MongoIterable<String> = mock {
        on { iterator() }.doReturn(mongoCursor)
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
    private val containerUserDAO: ContainerUserDAO = mock()
    private val securityContext: SecurityContext = mock()

    @Disabled
    @Test
    fun `create container works as expected`() {
        configuration.databaseName = "annorepo"
        println(configuration.databaseName)
        println(client.getDatabase(configuration.databaseName))
        val r =
            W3CResource(
                client = client,
                configuration = configuration,
                containerUserDAO = containerUserDAO
            )
        val response = r.createContainer(
            containerSpecs = ContainerSpecs(
                context = mutableListOf(),
                type = listOf(),
                label = "container label"
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

