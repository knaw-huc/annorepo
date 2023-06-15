package nl.knaw.huc.annorepo.resources

import java.net.URI
import java.util.*
import jakarta.annotation.security.PermitAll
import jakarta.ws.rs.*
import jakarta.ws.rs.core.*
import jakarta.ws.rs.core.MediaType.APPLICATION_JSON
import kotlin.math.min
import com.codahale.metrics.annotation.Timed
import com.fasterxml.jackson.databind.ObjectMapper
import com.mongodb.client.MongoClient
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import org.slf4j.LoggerFactory
import nl.knaw.huc.annorepo.api.*
import nl.knaw.huc.annorepo.api.ARConst.SECURITY_SCHEME_NAME
import nl.knaw.huc.annorepo.api.ResourcePaths.GLOBAL_SERVICES
import nl.knaw.huc.annorepo.auth.ContainerUserDAO
import nl.knaw.huc.annorepo.config.AnnoRepoConfiguration
import nl.knaw.huc.annorepo.resources.tools.*
import nl.knaw.huc.annorepo.service.UriFactory

@Path(GLOBAL_SERVICES)
@Produces(APPLICATION_JSON)
@PermitAll
@SecurityRequirement(name = SECURITY_SCHEME_NAME)
class GlobalServiceResource(
    private val configuration: AnnoRepoConfiguration,
    client: MongoClient,
    private val containerUserDAO: ContainerUserDAO,
    private val searchManager: SearchManager,
    private val uriFactory: UriFactory
) : AbstractContainerResource(configuration, client, ContainerAccessChecker(containerUserDAO)) {

    private val aggregateStageGenerator = AggregateStageGenerator(configuration)

    private val log = LoggerFactory.getLogger(javaClass)

    @Operation(description = "Find annotations in accessible containers matching the given query")
    @Timed
    @POST
    @Path("search")
    @Consumes(APPLICATION_JSON)
    fun createSearch(
        queryJson: String,
        @Context context: SecurityContext,
    ): Response {
        try {
            val queryMap = ObjectMapper().readValue(queryJson, HashMap::class.java)

            val aggregateStages =
                queryMap.toMap().map { (k, v) -> aggregateStageGenerator.generateStage(k, v) }.toList()
            val containerNames = accessibleContainers(context.userPrincipal.name)
            val task: SearchTask =
                searchManager.startGlobalSearch(
                    containerNames = containerNames,
                    queryMap = queryMap,
                    aggregateStages = aggregateStages
                )
            val id = task.id
            val location = uriFactory.globalSearchURL(id)
            return Response.created(location)
                .link(uriFactory.globalSearchStatusURL(id), "status")
                .entity(task.status.summary())
                .build()
        } catch (e: Exception) {
            throw BadRequestException(e.message)
        }
    }

    @Operation(description = "Get the given global search result page")
    @Timed
    @GET
    @Path("search/{searchId}")
    fun getSearchResultPage(
        @PathParam("searchId") searchId: String,
        @QueryParam("page") page: Int = 0,
        @Context context: SecurityContext,
    ): Response {
        val searchTaskStatus = searchManager.getSearchTask(searchId)?.status ?: throw NotFoundException()
        return when (searchTaskStatus.state) {
            SearchTask.State.DONE -> annotationPageResponse(searchTaskStatus, page, searchId)
            SearchTask.State.FAILED -> serverErrorResponse(searchTaskStatus)
            else -> acceptedResponse(searchTaskStatus)
        }
    }

    private fun acceptedResponse(searchTaskStatus: SearchTask.Status): Response =
        Response.accepted().entity(searchTaskStatus.summary()).build()

    private fun serverErrorResponse(searchTaskStatus: SearchTask.Status): Response =
        Response.serverError().entity(searchTaskStatus.summary()).build()

    private fun annotationPageResponse(
        searchTaskStatus: SearchTask.Status,
        page: Int,
        searchId: String
    ): Response {
        val total = searchTaskStatus.annotations.size
        val selection = searchTaskStatus.annotations.subList(
            page * configuration.pageSize,
            min((page + 1) * configuration.pageSize, total)
        )
        val annotationPage =
            buildAnnotationPage(
                searchUri = uriFactory.globalSearchURL(searchId),
                annotations = selection,
                page = page,
                total = total
            )
        return Response.ok(annotationPage).build()
    }

    @Operation(description = "Get information about the given global search")
    @Timed
    @GET
    @Path("search/{searchId}/status")
    fun getSearchStatus(
        @PathParam("searchId") searchId: String,
        @Context context: SecurityContext,
    ): Response {
        val searchTask = searchManager.getSearchTask(searchId) ?: throw NotFoundException()
        return Response.ok(searchTask.status.summary()).build()
    }

    private fun accessibleContainers(name: String): List<String> =
        containerUserDAO.getUserRoles(name).map { it.containerName }.toList()

    private fun buildAnnotationPage(
        searchUri: URI, annotations: AnnotationList, page: Int, total: Int,
    ): AnnotationPage {
        val prevPage = if (page > 0) {
            page - 1
        } else {
            null
        }
        val startIndex = configuration.pageSize * page
        val nextPage = if (startIndex + annotations.size < total) {
            page + 1
        } else {
            null
        }

        return AnnotationPage(
            context = listOf(ANNO_JSONLD_URL),
            id = searchPageUri(searchUri, page),
            partOf = searchUri.toString(),
            startIndex = startIndex,
            items = annotations,
            prev = if (prevPage != null) searchPageUri(searchUri, prevPage) else null,
            next = if (nextPage != null) searchPageUri(searchUri, nextPage) else null
        )
    }

    private fun searchPageUri(searchUri: URI, page: Int) =
        UriBuilder.fromUri(searchUri).queryParam("page", page).build().toString()

//    sorting the results?
}

