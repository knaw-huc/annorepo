package nl.knaw.huc.annorepo.resources

import jakarta.annotation.security.PermitAll
import jakarta.validation.constraints.NotNull
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.NotAuthorizedException
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.Context
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.core.SecurityContext
import com.codahale.metrics.annotation.Timed
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import nl.knaw.huc.annorepo.api.ARConst.SECURITY_SCHEME_NAME
import nl.knaw.huc.annorepo.api.RejectedUserEntry
import nl.knaw.huc.annorepo.api.ResourcePaths.ADMIN
import nl.knaw.huc.annorepo.api.UserEntry
import nl.knaw.huc.annorepo.auth.RootUser
import nl.knaw.huc.annorepo.dao.FIELD_API_KEY
import nl.knaw.huc.annorepo.dao.FIELD_USER_NAME
import nl.knaw.huc.annorepo.dao.UserDAO

@Path(ADMIN)
@Produces(MediaType.APPLICATION_JSON)
@PermitAll
@SecurityRequirement(name = SECURITY_SCHEME_NAME)
class AdminResource(
    private val userDAO: UserDAO,
) {
    @Operation(description = "Get username, api-key for all registered users")
    @Timed
    @GET
    @Path("users")
    fun getUsers(@Context context: SecurityContext): Response {
        assertUserIsRoot(context)
        val users = userDAO.allUserEntries()
        return Response.ok(users).build()
    }

    @Operation(description = "Get the names of the groups with the ability to create containers")
    @Timed
    @GET
    @Path("groups")
    fun getGroups(@Context context: SecurityContext): Response {
        assertUserIsRoot(context)
        val groups = userDAO.allGroupNames()
        return Response.ok(groups).build()
    }

    @Operation(description = "Add a user")
    @Timed
    @POST
    @Path("users")
    @Consumes(MediaType.APPLICATION_JSON)
    fun addUsers(
        @Context context: SecurityContext,
        @NotNull body: List<Map<String, String>>,
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
        val result = userDAO.addUserEntries(correctEntries)
        val entity = result.copy(rejected = rejectedEntries.plus(result.rejected))
        return Response.ok(entity).build()
    }

    @Operation(description = "Delete the user with the given userName")
    @Timed
    @DELETE
    @Path("users/{userName}")
    fun deleteUser(@Context context: SecurityContext, @PathParam("userName") userName: String): Response {
        assertUserIsRoot(context)
        val userWasDeleted = userDAO.deleteUsersByName(listOf(userName))
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

