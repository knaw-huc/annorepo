package nl.knaw.huc.annorepo.resources

import java.io.StringReader
import java.net.URI
import jakarta.annotation.security.PermitAll
import jakarta.json.Json
import jakarta.ws.rs.BadRequestException
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.NotAuthorizedException
import jakarta.ws.rs.NotFoundException
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.Context
import jakarta.ws.rs.core.MediaType.APPLICATION_JSON
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.core.SecurityContext
import jakarta.ws.rs.core.UriBuilder
import kotlin.math.min
import com.codahale.metrics.annotation.Timed
import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.mongodb.client.model.Filters.`in`
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import org.bson.Document
import org.bson.types.ObjectId
import nl.knaw.huc.annorepo.api.ANNO_JSONLD_URL
import nl.knaw.huc.annorepo.api.ARConst
import nl.knaw.huc.annorepo.api.ARConst.SECURITY_SCHEME_NAME
import nl.knaw.huc.annorepo.api.AnnotationPage
import nl.knaw.huc.annorepo.api.CustomQuerySpecs
import nl.knaw.huc.annorepo.api.PropertySet
import nl.knaw.huc.annorepo.api.QueryAsMap
import nl.knaw.huc.annorepo.api.ResourcePaths.CUSTOM_QUERY
import nl.knaw.huc.annorepo.api.ResourcePaths.EXPAND
import nl.knaw.huc.annorepo.api.ResourcePaths.GLOBAL_SERVICES
import nl.knaw.huc.annorepo.api.ResourcePaths.SEARCH
import nl.knaw.huc.annorepo.api.ResourcePaths.STATUS
import nl.knaw.huc.annorepo.api.WebAnnotationAsMap
import nl.knaw.huc.annorepo.api.optional
import nl.knaw.huc.annorepo.api.required
import nl.knaw.huc.annorepo.auth.RootUser
import nl.knaw.huc.annorepo.config.AnnoRepoConfiguration
import nl.knaw.huc.annorepo.dao.ContainerDAO
import nl.knaw.huc.annorepo.dao.ContainerUserDAO
import nl.knaw.huc.annorepo.dao.CustomQuery
import nl.knaw.huc.annorepo.dao.CustomQueryDAO
import nl.knaw.huc.annorepo.resources.tools.AggregateStageGenerator
import nl.knaw.huc.annorepo.resources.tools.AnnotationList
import nl.knaw.huc.annorepo.resources.tools.ContainerAccessChecker
import nl.knaw.huc.annorepo.resources.tools.CustomQueryTools
import nl.knaw.huc.annorepo.resources.tools.CustomQueryTools.extractParameterNames
import nl.knaw.huc.annorepo.resources.tools.CustomQueryTools.interpolate
import nl.knaw.huc.annorepo.resources.tools.CustomQueryTools.isValidQueryName
import nl.knaw.huc.annorepo.resources.tools.SearchChore
import nl.knaw.huc.annorepo.resources.tools.SearchManager
import nl.knaw.huc.annorepo.resources.tools.annotationCollectionLink
import nl.knaw.huc.annorepo.resources.tools.simplify
import nl.knaw.huc.annorepo.service.UriFactory

@Path(GLOBAL_SERVICES)
@Produces(APPLICATION_JSON)
@PermitAll
@SecurityRequirement(name = SECURITY_SCHEME_NAME)
class GlobalServiceResource(
    private val configuration: AnnoRepoConfiguration,
    private val containerDAO: ContainerDAO,
    private val containerUserDAO: ContainerUserDAO,
    private val customQueryDAO: CustomQueryDAO,
    private val searchManager: SearchManager,
    private val uriFactory: UriFactory
) : AbstractContainerResource(configuration, containerDAO, ContainerAccessChecker(containerUserDAO)) {

    private val aggregateStageGenerator = AggregateStageGenerator(configuration)
    private val objectMapper = jacksonObjectMapper()

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
            val queryMap: QueryAsMap = Json.createReader(StringReader(queryJson)).readObject().toMap().simplify()

            val aggregateStages =
                queryMap.toMap()
                    .map { (k, v) -> aggregateStageGenerator.generateStage(k, v) }
                    .toList()
            val containerNames = accessibleContainers(context.userPrincipal.name)
            val chore: SearchChore =
                searchManager.startGlobalSearch(
                    containerNames = containerNames,
                    queryMap = queryMap,
                    aggregateStages = aggregateStages
                )
            val id = chore.id
            val location = uriFactory.globalSearchURL(id)
            return Response.created(location)
                .link(uriFactory.globalSearchStatusURL(id), "status")
                .entity(chore.status.summary())
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
        @QueryParam("page") page: Int = 0
    ): Response {
        val searchChoreStatus = searchManager.getSearchChore(searchId)?.status ?: throw NotFoundException()
        searchManager.ping(searchId)
        return when (searchChoreStatus.state) {
            SearchChore.State.DONE -> annotationPageResponse(searchChoreStatus, page, searchId)
            SearchChore.State.FAILED -> serverErrorResponse(searchChoreStatus)
            else -> acceptedResponse(searchChoreStatus)
        }
    }

    @Operation(description = "Get information about the given global search")
    @Timed
    @GET
    @Path("$SEARCH/{searchId}/$STATUS")
    fun getSearchStatus(
        @PathParam("searchId") searchId: String,
    ): Response {
        val searchChore = searchManager.getSearchChore(searchId) ?: throw NotFoundException()
        return Response.ok(searchChore.status.summary()).build()
    }

    @Operation(description = "Create a custom query")
    @Timed
    @POST
    @Path(CUSTOM_QUERY)
    @Consumes(APPLICATION_JSON)
    fun createCustomQuery(
        customQuerySettings: CustomQuerySpecs,
        @Context context: SecurityContext,
    ): Response {
        context.checkUserHasAdminRights()
        val userName = context.userPrincipal?.name ?: ""
//        val settings = parseJson(customQueryJson)
        val settings = customQuerySettings
        if (customQueryDAO.nameIsTaken(settings.name)) {
            throw BadRequestException("A custom query with the name '${settings.name}' already exists")
        }
        if (!settings.name.isValidQueryName()) {
            throw BadRequestException("'${settings.name}' is invalid; the query name should contain alphanumerics only")
        }
        val queryTemplate = objectMapper.writeValueAsString(settings.query)
        val parameters = queryTemplate.extractParameterNames()
        val customQuery = CustomQuery(
            name = settings.name,
            description = settings.description,
            label = settings.label,
            queryTemplate = queryTemplate,
            parameters = parameters,
            createdBy = userName,
            public = settings.public ?: true
        )
        customQueryDAO.store(customQuery)
        return Response.created(uriFactory.customQueryURL(settings.name)).build()
    }

    @Operation(description = "Read a custom query")
    @Timed
    @GET
    @Path("$CUSTOM_QUERY/{customQueryName}")
    @Consumes(APPLICATION_JSON)
    fun getCustomQuery(
        @PathParam("customQueryName") customQueryName: String,
        @Context context: SecurityContext,
    ): Response {
        val customQuery = customQueryDAO.getByName(customQueryName) ?: throw NotFoundException()
        if (!customQuery.public) {
            context.checkUserHasAdminRights()
        }
        return Response.ok(customQuery).build()
    }

    @Operation(description = "Delete a custom query")
    @Timed
    @DELETE
    @Path("$CUSTOM_QUERY/{customQueryName}")
    fun deleteCustomQuery(
        @PathParam("customQueryName") customQueryName: String,
        @Context context: SecurityContext,
    ): Response {
        context.checkUserHasAdminRights()
        val customQuery = customQueryDAO.getByName(customQueryName) ?: throw NotFoundException()
        val creatorName = customQuery.createdBy
        val userName = context.userPrincipal.name
        if (creatorName == userName || context.userPrincipal is RootUser) {
            customQueryDAO.deleteByName(customQueryName)
            return Response.noContent().build()
        }
        throw NotAuthorizedException("This user is not authorized to delete this custom query")
    }

    @Operation(description = "Show custom query with parameters filled in")
    @Timed
    @GET
    @Path("$CUSTOM_QUERY/{customQueryCall}/$EXPAND")
    @Consumes(APPLICATION_JSON)
    fun getExpandedCustomQuery(
        @PathParam("customQueryCall") customQueryCall: String,
        @Context context: SecurityContext,
    ): Response {
        val (customQueryName, parameters) = CustomQueryTools.decode(customQueryCall)
            .getOrElse { throw BadRequestException(it.message) }
        val customQuery = customQueryDAO.getByName(customQueryName) ?: throw NotFoundException()
        if (!customQuery.public) {
            context.checkUserHasAdminRights()
        }
        val missingParameters = customQuery.parameters.toSet() - parameters.keys
        if (missingParameters.isNotEmpty()) {
            throw BadRequestException("No values given for parameter(s): $missingParameters")
        }
        val expanded = customQuery.queryTemplate.interpolate(parameters)
        return Response.ok(expanded).build()
    }

    @Operation(description = "List all custom queries")
    @Timed
    @GET
    @Path(CUSTOM_QUERY)
    @Consumes(APPLICATION_JSON)
    fun getCustomQueries(
        @Context context: SecurityContext,
    ): Response {
        context.checkUserHasAdminRights()
        val allQueries = customQueryDAO.getAllCustomQueries()
        return Response.ok(allQueries).build()
    }

    private fun parseJson(jsonString: String): CustomQuerySpecs {
        try {
            val propertySet: PropertySet = objectMapper.readValue<PropertySet>(jsonString)
            val keys = propertySet.keys
            if (keys.contains("name") && keys.contains("query")) {
                val name = propertySet.required<String>("name")
                val query = propertySet.required<PropertySet>("query")
                val public: Boolean? = propertySet.optional<Boolean>("public")
                val label: String? = propertySet.optional<String>("label")
                val description: String? = propertySet.optional<String>("description")
                return CustomQuerySpecs(
                    name = name,
                    query = query,
                    public = public,
                    label = label,
                    description = description
                )
            } else {
                throw BadRequestException("invalid customQueryJson: no 'name' and/or 'query' fields found")
            }
        } catch (e: JsonParseException) {
            throw BadRequestException("invalid json: ${e.message}")
        }
    }

    private fun acceptedResponse(searchChoreStatus: SearchChore.Status): Response =
        Response.accepted().entity(searchChoreStatus.summary()).build()

    private fun serverErrorResponse(searchChoreStatus: SearchChore.Status): Response =
        Response.serverError().entity(searchChoreStatus.summary()).build()

    private fun annotationPageResponse(
        searchChoreStatus: SearchChore.Status,
        page: Int,
        searchId: String
    ): Response {
        val total = searchChoreStatus.annotationIds.size
        val annotationIdSelection = searchChoreStatus.annotationIds.subList(
            page * configuration.pageSize,
            min((page + 1) * configuration.pageSize, total)
        )
        val selection: MutableList<WebAnnotationAsMap> = mutableListOf()
        val grouped = annotationIdSelection.groupBy { it.collectionName }
        for (collectionName in grouped.keys) {
            val objectIds: List<ObjectId> = grouped[collectionName]?.map { it.objectId } ?: listOf()
            containerDAO.getCollection(collectionName)
                .find(`in`("_id", objectIds))
                .map { toAnnotationMap(it, collectionName) }
                .forEach(selection::add)
        }
        val annotationPage =
            buildAnnotationPage(
                searchUri = uriFactory.globalSearchURL(searchId),
                annotations = selection,
                page = page,
                total = total
            )
        return Response.ok(annotationPage).build()
    }

    private fun toAnnotationMap(a: Document, containerName: String): WebAnnotationAsMap =
        a.get(ARConst.ANNOTATION_FIELD, Document::class.java)
            .toMutableMap()
            .apply<MutableMap<String, Any>> {
                put(
                    "id", uriFactory.annotationURL(containerName, a.getString(ARConst.ANNOTATION_NAME_FIELD))
                )
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
            partOf = annotationCollectionLink(searchUri.toString()),
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

