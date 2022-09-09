package nl.knaw.huc.annorepo.resources

import com.codahale.metrics.annotation.Timed
import io.swagger.v3.oas.annotations.Operation
import nl.knaw.huc.annorepo.api.ResourcePaths.ADMIN
import nl.knaw.huc.annorepo.auth.FIELD_API_KEY
import nl.knaw.huc.annorepo.auth.FIELD_USER_NAME
import nl.knaw.huc.annorepo.auth.RejectedUserEntry
import nl.knaw.huc.annorepo.auth.RootUser
import nl.knaw.huc.annorepo.auth.UserDTO
import nl.knaw.huc.annorepo.auth.UserEntry
import org.slf4j.LoggerFactory
import javax.annotation.security.PermitAll
import javax.validation.constraints.NotNull
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.NotAuthorizedException
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.Context
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.SecurityContext

@Path(ADMIN)
@Produces(MediaType.APPLICATION_JSON)
@PermitAll
class AdminResource(
    private val userDTO: UserDTO
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Operation(description = "Get username, api-key for all registered users")
    @Timed
    @GET
    @Path("users")
    fun getUsers(@Context context: SecurityContext): Response {
        assertUserIsRoot(context)
        val users = userDTO.allUserEntries()
        return Response.ok(users).build()
    }

    @Operation(description = "Add a user")
    @Timed
    @POST
    @Path("users")
    @Consumes(MediaType.APPLICATION_JSON)
    fun addUsers(
        @Context context: SecurityContext,
        @NotNull body: List<Map<String, String>>
    ): Response {
        assertUserIsRoot(context)
        val correctEntries = mutableListOf<UserEntry>()
        val rejectedEntries = mutableListOf<RejectedUserEntry>()
        for (entry in body) {
            if (entry.containsKey(FIELD_USER_NAME) && entry.containsKey(FIELD_API_KEY)) {
                val userName = entry[FIELD_USER_NAME]!!
                val apiKey = entry[FIELD_API_KEY]!!
                correctEntries.add(UserEntry(userName = userName, apiKey = apiKey))
            } else {
                rejectedEntries.add(
                    RejectedUserEntry(
                        userEntry = entry.toMap(),
                        reason = "missing field(s): $FIELD_USER_NAME and/or $FIELD_API_KEY"
                    )
                )
            }
        }
        val result = userDTO.addUserEntries(correctEntries)
        val entity = result.copy(rejected = rejectedEntries.plus(result.rejected))
        return Response.ok(entity).build()
    }

    @Operation(description = "Delete the user with the given userName")
    @Timed
    @DELETE
    @Path("users/{userName}")
    fun deleteUser(@Context context: SecurityContext, @PathParam("userName") userName: String): Response {
        assertUserIsRoot(context)
        val userWasDeleted = userDTO.deleteUsersByName(listOf(userName))
        return if (userWasDeleted) {
            Response.noContent().build()
        } else {
            Response.status(Response.Status.NOT_FOUND).entity("no user found with that name").build()
        }
    }

    private fun assertUserIsRoot(context: SecurityContext) {
        if (context.userPrincipal !is RootUser) {
            throw NotAuthorizedException("This endpoint is for the root user only")
        }
    }

}

