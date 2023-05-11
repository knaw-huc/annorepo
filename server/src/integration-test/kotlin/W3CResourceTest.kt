import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport
import io.dropwizard.testing.junit5.ResourceExtension
import nl.knaw.huc.annorepo.api.ARConst.ANNOTATION_MEDIA_TYPE
import nl.knaw.huc.annorepo.api.ContainerSpecs
import nl.knaw.huc.annorepo.api.ResourcePaths.SEARCH
import nl.knaw.huc.annorepo.api.ResourcePaths.SERVICES
import nl.knaw.huc.annorepo.api.ResourcePaths.W3C
import nl.knaw.huc.annorepo.config.AnnoRepoConfiguration
import nl.knaw.huc.annorepo.resources.ServiceResource
import nl.knaw.huc.annorepo.resources.W3CResource
import org.assertj.core.api.Assertions.assertThat
import org.bson.Document
import org.eclipse.jetty.http.HttpStatus
import org.eclipse.jetty.http.HttpStatus.CREATED_201
import org.eclipse.jetty.http.HttpStatus.OK_200
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.litote.kmongo.KMongo
import org.mockito.Mockito.mock
import javax.ws.rs.client.Entity.json
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.MediaType.APPLICATION_JSON

private const val BASE_URI = "https://annorepo.com"

@ExtendWith(DropwizardExtensionsSupport::class)
class W3CResourceTest {
    private val db: MongoDatabase = mock(MongoDatabase::class.java)
    private val dbCollection: MongoCollection<*> = mock(MongoCollection::class.java)

    private val client = KMongo.createClient("mongodb://localhost/")

    //    private val client = mock(MongoClient::class.java)
    private val config: AnnoRepoConfiguration = AnnoRepoConfiguration().apply { externalBaseUrl = BASE_URI }
    private val resource = ResourceExtension.builder()
        .addResource(W3CResource(config, client))
//        .addResource(SearchResource(config, client))
        .addResource(ServiceResource(config, client))
        .build()

    @Test
    fun test() {
        val name = "containername"
        val builder = resource.client()
            .target("/$W3C/")
            .request(ANNOTATION_MEDIA_TYPE)
            .header("Slug", name)
        val json = json(
            ContainerSpecs(label = "label", context = listOf(), type = listOf()),
        )
        val createResponse = builder.post(json)
        println("response=$createResponse")
        assertThat(createResponse.status).isEqualTo(HttpStatus.CREATED_201)
        println(
            createResponse.readEntity(HashMap::class.java)
        )
        val deleteResponse = resource.client().target("/$W3C/$name").request(MediaType.APPLICATION_JSON).delete()
        assertThat(deleteResponse.status).isEqualTo(HttpStatus.NO_CONTENT_204)

        val readResponse = resource.client().target("/$W3C/$name").request(MediaType.APPLICATION_JSON).get()
        println(readResponse.status)
        println(readResponse.readEntity(HashMap::class.java))
//        assertThat(readResponse.status).isEqualTo(HttpStatus.SC_NOT_FOUND)
    }

    @Test
    fun `search with range overlap and excluded field values`() {
        val query = """
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
        val name = "2b958f4-e66b-40dd-bf90-923849ec5540"
        val searchResponse = resource.client()
            .target("/$SERVICES/$name/$SEARCH")
            .request(APPLICATION_JSON)
            .post(json(Document.parse(query)))
        println("response=$searchResponse")
        assertThat(searchResponse.status).isEqualTo(CREATED_201)
        println(
            searchResponse.readEntity(String::class.java)
        )
        println(searchResponse.headers)
        val locations = searchResponse.headers["location"] as List<String>
        val location = locations[0]
        println(location)
        val searchId = location.split("/").last()
        println(searchId)
        val searchResultResponse = resource.client()
            .target("/$SERVICES/$name/$SEARCH/$searchId")
            .request(APPLICATION_JSON)
            .get()
        println("response=$searchResultResponse")
        println(
            searchResultResponse.readEntity(String::class.java)
        )

    }

    @Test
    fun `test Search Annotations`() {
        val name = "2b958f4-e66b-40dd-bf90-923849ec5540"
        val query = """
            {
                "target.selector.type": "urn:example:republic:TextAnchorSelector",
                "type": "Annotation"
            }
        """.trimIndent()
        val searchResponse = resource.client()
            .target("/search/$name/annotations")
            .request(ANNOTATION_MEDIA_TYPE)
            .post(json(Document.parse(query)))
        println("response=$searchResponse")
        assertThat(searchResponse.status).isEqualTo(OK_200)
        println(
            searchResponse.readEntity(String::class.java)
        )
    }

    @Test
    fun `test Search Annotations Within Range`() {
        val name = "searchbyrange"
        val searchResponse =
            resource.client()
                .target("/search/$name/within_range")
                .queryParam("target.source", "urn:textrepo:text_x")
                .queryParam("range.start", 10)
                .queryParam("range.end", 300)
                .request()
                .get()
        println("response=$searchResponse")
        assertThat(searchResponse.status).isEqualTo(OK_200)
        val resultPage = searchResponse.readEntity(Document::class.java)
        val list = getAnnotationList(resultPage)
        println(list)
        assertThat(list).hasSize(2)
    }

    @Test
    fun `test Search Annotations Overlapping With Range`() {
        val name = "searchbyrange"
        val searchResponse = resource.client().target("/search/$name/overlapping_with_range")
            .queryParam("target.source", "urn:textrepo:text_x").queryParam("range.start", 10)
            .queryParam("range.end", 300).request().get()
        println("response=$searchResponse")
        assertThat(searchResponse.status).isEqualTo(OK_200)
        val resultPage = searchResponse.readEntity(Document::class.java)
        val list = getAnnotationList(resultPage)
        println(list)
        assertThat(list).hasSize(4)
    }

    private fun getAnnotationList(resultPage: Document): ArrayList<*> {
        return resultPage.get("as:items", Map::class.java)["@list"] as ArrayList<*>
    }
}