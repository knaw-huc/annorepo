package nl.knaw.huc.annorepo.resources

import com.codahale.metrics.annotation.Timed
import io.swagger.v3.oas.annotations.Hidden
import io.swagger.v3.oas.annotations.Operation
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

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
        val resourceAsStream = Thread.currentThread().contextClassLoader.getResourceAsStream("index.html")
        return Response.ok(resourceAsStream)
            .header("Pragma", "public")
            .header("Cache-Control", "public")
            .build()
    }

    @GET
    @Path("favicon.ico")
    @Operation(description = "Placeholder for favicon.ico")
    fun getFavIcon(): Response = Response.noContent().build()

    @GET
    @Path("robots.txt")
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(description = "Placeholder for robots.txt")
    fun noRobots(): String = "User-agent: *\nDisallow: /\n"
}
