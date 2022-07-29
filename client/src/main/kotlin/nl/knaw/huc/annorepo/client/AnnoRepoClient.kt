package nl.knaw.huc.annorepo.client

import java.net.URI
import javax.ws.rs.client.ClientBuilder
import javax.ws.rs.client.WebTarget

class AnnoRepoClient(serverURI: URI) {
    private val webTarget: WebTarget = ClientBuilder.newClient()
        .target(serverURI)

    fun getAbout(): Map<String, Any> =
        webTarget.path("about")
            .request()
            .get(Map::class.java) as Map<String, Any>
}