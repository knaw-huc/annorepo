package nl.knaw.huc.annorepo.resources

import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.HttpHeaders
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import com.codahale.metrics.annotation.Timed
import io.swagger.v3.oas.annotations.Hidden
import io.swagger.v3.oas.annotations.Operation
import nl.knaw.huc.annorepo.resources.tools.ResourceLoader

@Hidden
@Path("/")
class HomePageResource {
    /**
     * Shows the homepage for the backend
     *
     * @return HTML representation of the homepage
     */
    @GET
    @Operation(description = "Show the server homepage")
    @Produces(MediaType.TEXT_HTML)
    @Timed
    fun getHomePage(): Response {
        val resourceAsStream = ResourceLoader.asStream("index.html")
        return Response.ok(resourceAsStream)
            .header("Pragma", "public")
            .header(HttpHeaders.CACHE_CONTROL, "public")
            .build()
    }

    @GET
    @Path("favicon.ico")
    fun getFavIcon(): Response = Response.ok(ResourceLoader.asStream("favicon.ico"))
        .header(HttpHeaders.CACHE_CONTROL, "public")
        .build()

    @GET
    @Path("robots.txt")
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(description = "Placeholder for robots.txt")
    fun noRobots(): String = "${HttpHeaders.USER_AGENT}: *\nDisallow: /\n"
}
