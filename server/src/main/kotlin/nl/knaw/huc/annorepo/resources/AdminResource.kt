package nl.knaw.huc.annorepo.resources

import com.codahale.metrics.annotation.Timed
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import nl.knaw.huc.annorepo.api.ARConst.SECURITY_SCHEME_NAME
import nl.knaw.huc.annorepo.api.ResourcePaths.ADMIN
import nl.knaw.huc.annorepo.auth.FIELD_API_KEY
import nl.knaw.huc.annorepo.auth.FIELD_USER_NAME
import nl.knaw.huc.annorepo.auth.RejectedUserEntry
import nl.knaw.huc.annorepo.auth.RootUser
import nl.knaw.huc.annorepo.auth.UserDAO
import nl.knaw.huc.annorepo.auth.UserEntry
import org.slf4j.LoggerFactory
import java.net.URI
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import javax.annotation.security.PermitAll
import javax.validation.constraints.NotNull
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.NotAuthorizedException
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.Context
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.SecurityContext
import kotlin.random.Random

@Path(ADMIN)
@Produces(MediaType.APPLICATION_JSON)
@PermitAll
@SecurityRequirement(name = SECURITY_SCHEME_NAME)
class AdminResource(
    private val userDAO: UserDAO,
    private val executorService: ExecutorService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Operation(
        description = "Get username, api-key for all registered users",
        parameters = [Parameter(
            name = "Authorization",
            `in` = ParameterIn.HEADER,
            description = "The bearer token",
            required = true
        )]
    )
    @Timed
    @GET
    @Path("users")
    fun getUsers(@Context context: SecurityContext): Response {
        assertUserIsRoot(context)
        val users = userDAO.allUserEntries()
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

    val futures = mutableListOf<Future<String>>()

    @Operation(description = "Start a test")
    @Timed
    @PUT
    @Path("test")
    fun startTest(@Context context: SecurityContext): Response {
        assertUserIsRoot(context)
        val callableTask: Callable<String> = Callable<String> {
            val seconds = Random.nextLong(1L, 30L)
            println("waiting $seconds seconds")
            TimeUnit.SECONDS.sleep(seconds)
            "Task ran for $seconds seconds"
        }
        val future = executorService.submit(callableTask)
        futures.add(future)
        val location = URI.create("http://localhost:8080/admin/test")
        return Response.created(location).build()
    }

    @Operation(description = "Get the test status")
    @Timed
    @GET
    @Path("test")
    fun getTest(@Context context: SecurityContext): Response {
        assertUserIsRoot(context)
        val tasks = mutableListOf<MutableMap<String, String>>()
        for (f in futures) {
            val taskInfo = mutableMapOf<String, String>()
            val status = when {
                f.isDone -> "done"
                f.isCancelled -> "cancelled"
                else -> "running"
            }
            taskInfo["status"] = status
            val result = when {
                f.isDone -> f.get()
                else -> null
            }
            if (f.isDone) {
                taskInfo["result"] = f.get()
            }
            tasks.add(taskInfo)
        }
        val testStatus = mapOf("tasks" to tasks)
        return Response.ok(testStatus).build()
    }

    private fun assertUserIsRoot(context: SecurityContext) {
        if (context.userPrincipal !is RootUser) {
            throw NotAuthorizedException("This endpoint is for the root user only")
        }
    }
}

