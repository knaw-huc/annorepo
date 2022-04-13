package nl.knaw.huc.annorepo.resources

import com.codahale.metrics.annotation.Timed
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import nl.knaw.huc.annorepo.api.ResourcePaths
import org.jdbi.v3.core.Jdbi
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(ResourcePaths.W3C)
@Path(ResourcePaths.W3C)
@Produces(MediaType.APPLICATION_JSON)
class W3CResource(jdbi: Jdbi) {

    @ApiOperation(value = "Show link to the W3C Web Annotation Protocol")
    @Timed
    @GET
    @Produces(MediaType.TEXT_HTML)
    fun getW3CInfo() =
        """This is the endpoint for the <a href="https://www.w3.org/TR/annotation-protocol/">W3C Web Annotation Protocol</a>"""

    @ApiOperation(value = "Create an Annotation Container")
    @Timed
    @POST
    fun createContainer() {

    }

    @ApiOperation(value = "Get an Annotation Container")
    @Timed
    @GET
    fun readContainer() {

    }

}