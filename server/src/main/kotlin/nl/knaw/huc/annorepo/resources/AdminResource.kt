package nl.knaw.huc.annorepo.resources

import com.codahale.metrics.annotation.Timed
import com.mongodb.client.MongoClient
import com.mongodb.client.model.Indexes.ascending
import com.mongodb.client.model.Projections.fields
import com.mongodb.client.model.Projections.include
import io.swagger.v3.oas.annotations.Operation
import nl.knaw.huc.annorepo.api.ARConst.USER_COLLECTION
import nl.knaw.huc.annorepo.api.ResourcePaths.ADMIN
import nl.knaw.huc.annorepo.auth.FIELD_API_KEY
import nl.knaw.huc.annorepo.auth.FIELD_USER_NAME
import nl.knaw.huc.annorepo.auth.RootUser
import nl.knaw.huc.annorepo.config.AnnoRepoConfiguration
import org.bson.Document
import org.litote.kmongo.excludeId
import org.slf4j.LoggerFactory
import javax.annotation.security.PermitAll
import javax.validation.constraints.NotNull
import javax.ws.rs.GET
import javax.ws.rs.NotAuthorizedException
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.Context
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.SecurityContext

@Path(ADMIN)
@Produces(MediaType.APPLICATION_JSON)
@PermitAll
class AdminResource(
    private val configuration: AnnoRepoConfiguration,
    client: MongoClient
) {
    private val mdb = client.getDatabase(configuration.databaseName)
    private val userCollection = mdb.getCollection(USER_COLLECTION)
    private val log = LoggerFactory.getLogger(javaClass)

    @Operation(description = "Get username, api-key for all registered users")
    @Timed
    @GET
    @Path("users")
    fun getUsers(@Context context: SecurityContext): Response {
        assertUserIsRoot(context)
        val users = userCollection.find()
            .projection(fields(include(FIELD_USER_NAME, FIELD_API_KEY), excludeId()))
            .sort(ascending(FIELD_USER_NAME))
            .toList()
        return Response.ok(users).build()
    }

    @Operation(description = "Add a user")
    @Timed
    @POST
    @Path("users")
    fun addUser(
        @Context context: SecurityContext,
        @NotNull @QueryParam("name") userName: String,
        @NotNull @QueryParam("apiKey") apiKey: String
    ): Response {
        assertUserIsRoot(context)
        userCollection.insertOne(Document(FIELD_USER_NAME, userName).append(FIELD_API_KEY, apiKey))
        return Response.noContent().build()
    }

    private fun assertUserIsRoot(context: SecurityContext) {
        if (context.userPrincipal !is RootUser) {
            throw NotAuthorizedException("This endpoint is for the root user only")
        }
    }

}

