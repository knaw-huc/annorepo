package nl.knaw.huc.annorepo.resources

import java.io.StringReader
import java.time.Instant
import java.util.Date
import java.util.UUID
import jakarta.annotation.security.PermitAll
import jakarta.json.Json
import jakarta.ws.rs.BadRequestException
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.Context
import jakarta.ws.rs.core.EntityTag
import jakarta.ws.rs.core.MediaType.APPLICATION_JSON
import jakarta.ws.rs.core.Request
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.core.SecurityContext
import kotlin.collections.set
import kotlin.math.abs
import com.codahale.metrics.annotation.Timed
import com.mongodb.client.model.Aggregates
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.ReplaceOptions
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import org.bson.BSONException
import org.bson.Document
import org.bson.json.JsonParseException
import org.litote.kmongo.aggregate
import org.litote.kmongo.findOne
import org.litote.kmongo.json
import org.litote.kmongo.replaceOneWithFilter
import org.slf4j.LoggerFactory
import nl.knaw.huc.annorepo.api.ANNO_JSONLD_URL
import nl.knaw.huc.annorepo.api.ARConst.ANNOTATION_FIELD
import nl.knaw.huc.annorepo.api.ARConst.ANNOTATION_MEDIA_TYPE
import nl.knaw.huc.annorepo.api.ARConst.ANNOTATION_NAME_FIELD
import nl.knaw.huc.annorepo.api.ARConst.CONTAINER_NAME_FIELD
import nl.knaw.huc.annorepo.api.ARConst.SECURITY_SCHEME_NAME
import nl.knaw.huc.annorepo.api.AnnotationData
import nl.knaw.huc.annorepo.api.ContainerMetadata
import nl.knaw.huc.annorepo.api.ContainerPage
import nl.knaw.huc.annorepo.api.ContainerSpecs
import nl.knaw.huc.annorepo.api.IndexType
import nl.knaw.huc.annorepo.api.ResourcePaths
import nl.knaw.huc.annorepo.api.Role
import nl.knaw.huc.annorepo.api.WebAnnotationAsMap
import nl.knaw.huc.annorepo.config.AnnoRepoConfiguration
import nl.knaw.huc.annorepo.dao.ContainerDAO
import nl.knaw.huc.annorepo.dao.ContainerUserDAO
import nl.knaw.huc.annorepo.exceptions.PreconditionFailedException
import nl.knaw.huc.annorepo.resources.tools.ContainerAccessChecker
import nl.knaw.huc.annorepo.resources.tools.IndexManager
import nl.knaw.huc.annorepo.resources.tools.makeAnnotationETag
import nl.knaw.huc.annorepo.resources.tools.simplify
import nl.knaw.huc.annorepo.service.JsonLdUtils
import nl.knaw.huc.annorepo.service.UriFactory

private const val RESOURCE_LINK = "http://www.w3.org/ns/ldp#Resource"
private const val ANNOTATION_LINK = "http://www.w3.org/ns/ldp#Annotation"

@Path(ResourcePaths.W3C)
@Produces(ANNOTATION_MEDIA_TYPE)
@PermitAll
@SecurityRequirement(name = SECURITY_SCHEME_NAME)
class W3CResource(
    private val configuration: AnnoRepoConfiguration,
    private val containerDAO: ContainerDAO,
    private val containerUserDAO: ContainerUserDAO,
    private val uriFactory: UriFactory,
    private val indexManager: IndexManager
) : AbstractContainerResource(configuration, containerDAO, ContainerAccessChecker(containerUserDAO)) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Operation(description = "Create an Annotation Container")
    @Timed
    @POST
    @Consumes(ANNOTATION_MEDIA_TYPE, APPLICATION_JSON)
    fun createContainer(
        containerSpecs: ContainerSpecs,
        @HeaderParam("slug") slug: String?,
        @Context context: SecurityContext,
    ): Response {
//        log.debug("containerSpecs={}", containerSpecs)
        context.checkUserHasAdminRights()
        var containerName = slug ?: UUID.randomUUID().toString()
        if (containerDAO.containerExists(containerName)) {
            log.debug("A container with the suggested name $containerName already exists, generating a new name.")
            containerName = UUID.randomUUID().toString()
        }
        containerDAO.createCollection(containerName)
        setupCollectionMetadata(containerName, containerSpecs.label, containerSpecs.readOnlyForAnonymousUsers)
        if (configuration.withAuthentication) {
            val userName = context.userPrincipal.name
            containerUserDAO.addContainerUser(containerName, userName, Role.ADMIN)
        }
        indexManager.startIndexCreation(
            containerName = containerName,
            fieldName = ANNOTATION_NAME_FIELD,
            isJsonField = false,
            indexTypeName = "annotation_name",
            indexType = IndexType.HASHED
        )

        val containerData = getContainerPage(containerName, 0, configuration.pageSize)
        val uri = uriFactory.containerURL(containerName)
        val eTag = makeContainerETag(containerName)
        return Response.created(uri)
            .contentLocation(uri)
            .header("Vary", "Accept")
            .link("http://www.w3.org/ns/ldp#BasicContainer", "type")
            .link("http://www.w3.org/TR/annotation-protocol", "http://www.w3.org/ns/ldp#constrainedBy")
            .allow("POST", "GET", "DELETE", "OPTIONS", "HEAD")
            .tag(eTag)
            .entity(containerData).build()
    }

    @Operation(description = "Get an Annotation Container")
    @Timed
    @GET
    @Path("{containerName}")
    fun readContainer(
        @PathParam("containerName") containerName: String,
        @QueryParam("page") page: Int? = null,
        @Context context: SecurityContext,
    ): Response {
        log.debug("read Container $containerName, page $page")
        context.checkUserHasReadRightsInThisContainer(containerName)

        val containerPage = getContainerPage(containerName, page ?: 0, configuration.pageSize)
        val uri = uriFactory.containerURL(containerName)
        val eTag = makeContainerETag(containerName)
        return when {
            containerPage != null -> {
                val entity =
                    if (page == null) containerPage else containerPage.first.copy(context = listOf(ANNO_JSONLD_URL))
                Response.ok(entity)
                    .contentLocation(uri)
                    .header("Vary", "Accept")
                    .link("http://www.w3.org/ns/ldp#BasicContainer", "type")
                    .link("http://www.w3.org/TR/annotation-protocol/", "http://www.w3.org/ns/ldp#constrainedBy")
                    .allow("POST", "GET", "DELETE", "OPTIONS", "HEAD")
                    .tag(eTag)
                    .build()
            }

            else -> {
                Response.status(Response.Status.NOT_FOUND).entity("Container '$containerName' not found").build()
            }
        }

    }

    @Operation(description = "Delete an empty Annotation Container")
    @Timed
    @DELETE
    @Path("{containerName}")
    fun deleteContainer(
        @PathParam("containerName") containerName: String,
        @QueryParam("force") force: Boolean = false,
        @Context req: Request,
        @Context context: SecurityContext,
    ): Response {
        context.checkUserHasAdminRightsInThisContainer(containerName)
        val eTag = makeContainerETag(containerName)
        validateETag(req, eTag)
        val containerPage = getContainerPage(containerName, 0, configuration.pageSize)
        return when {
            (containerPage == null) -> {
                Response.status(Response.Status.NOT_FOUND).entity("Container $containerName was not found.").build()
            }

            (containerPage.total == 0L || force) -> {
                containerDAO.getCollection(containerName).drop()
                val containerMetadataCollection =
                    containerDAO.getContainerMetadataCollection()
                containerMetadataCollection.deleteOne(eq("name", containerName))
                containerUserDAO.getUsersForContainer(containerName).forEach { user ->
                    containerUserDAO.removeContainerUser(containerName, user.userName)
                }
                Response.noContent().build()
            }

            else -> {
                Response.status(Response.Status.BAD_REQUEST)
                    .entity(
                        "Container $containerName is not empty,"
                                + " all annotations need to be removed from this container first."
                    ).build()
            }
        }
    }

    @Operation(description = "Create an Annotation")
    @Timed
    @POST
    @Path("{containerName}")
    @Consumes(ANNOTATION_MEDIA_TYPE, APPLICATION_JSON)
    fun createAnnotation(
        @HeaderParam("slug") slug: String?,
        @PathParam("containerName") containerName: String,
        annotationJson: String,
        @Context context: SecurityContext,
    ): Response {
        context.checkUserHasEditRightsInThisContainer(containerName)

        var name = slug ?: UUID.randomUUID().toString()
        val container = containerDAO.getCollection(containerName)
        val existingAnnotationDocument = container.find(Document(ANNOTATION_NAME_FIELD, name)).first()
        if (existingAnnotationDocument != null) {
            log.warn(
                "An annotation with the suggested name $name already exists in container $containerName," +
                        " generating a new name."
            )
            name = UUID.randomUUID().toString()
        }
        try {
            val annotationDocument = Document.parse(annotationJson)
            val fields = JsonLdUtils.extractFields(annotationJson)
            updateFieldCount(containerName, fields, emptySet())
            val doc = Document(ANNOTATION_NAME_FIELD, name)
                .append(ANNOTATION_FIELD, annotationDocument)
            val r = container.insertOne(doc).insertedId?.asObjectId()?.value

            val annotationData = AnnotationData(
                r!!.timestamp.toLong(),
                name,
                doc.getEmbedded(listOf(ANNOTATION_FIELD), Document::class.java).toJson(),
                Date.from(Instant.now()),
                Date.from(Instant.now())
            )
            val uri = uriFactory.annotationURL(containerName, name)
            val eTag = makeAnnotationETag(containerName, name)
            val entity = annotationData.contentWithAssignedId(containerName, name)
            return Response.created(uri)
                .header("Vary", "Accept")
                .allow("POST", "PUT", "GET", "DELETE", "OPTIONS", "HEAD")
                .link(RESOURCE_LINK, "type")
                .link(ANNOTATION_LINK, "type")
                .tag(eTag)
                .entity(entity)
                .build()
        } catch (e: BSONException) {
            throw BadRequestException(jsonParseExceptionMessage(annotationJson, e))
        } catch (e: JsonParseException) {
            throw BadRequestException(jsonParseExceptionMessage(annotationJson, e))
        }
    }

    @Operation(description = "Get an Annotation")
    @Timed
    @GET
    @Path("{containerName}/{annotationName}")
    @Produces(ANNOTATION_MEDIA_TYPE)
    fun readAnnotation(
        @PathParam("containerName") containerName: String,
        @PathParam("annotationName") annotationName: String,
        @Context context: SecurityContext,
    ): Response {
        log.debug("read annotation $annotationName in container $containerName")
        context.checkUserHasReadRightsInThisContainer(containerName)

        val container = containerDAO.getCollection(containerName)
        val annotationDocument = container.find(Document(ANNOTATION_NAME_FIELD, annotationName)).first()
            ?.get(ANNOTATION_FIELD, Document::class.java)
        return if (annotationDocument != null) {
            val annotationData = AnnotationData(
                0L, annotationName, annotationDocument.toJson(), Date.from(Instant.now()), Date.from(Instant.now())
            )
            val entity = annotationData.contentWithAssignedId(containerName, annotationName)
            val eTag = makeAnnotationETag(containerName, annotationName)
            Response.ok(entity).header("Vary", "Accept").allow("POST", "PUT", "GET", "DELETE", "OPTIONS", "HEAD")
                .link(RESOURCE_LINK, "type").link(ANNOTATION_LINK, "type").lastModified(annotationData.modified)
                .tag(eTag).build()
        } else Response.status(Response.Status.NOT_FOUND).build()
    }

    @Operation(description = "Update an existing Annotation")
    @Timed
    @PUT
    @Path("{containerName}/{annotationName}")
    @Consumes(ANNOTATION_MEDIA_TYPE, APPLICATION_JSON)
    fun updateAnnotation(
        @PathParam("containerName") containerName: String,
        @PathParam("annotationName") annotationName: String,
        @Context req: Request,
        @Context context: SecurityContext,
        annotationJson: String,
    ): Response {
        log.debug("annotation=\n$annotationJson")
        context.checkUserHasEditRightsInThisContainer(containerName)

        val eTag = makeAnnotationETag(containerName, annotationName)
        validateETag(req, eTag)
        val annotationDocument = Document.parse(annotationJson)
        val newFields = JsonLdUtils.extractFields(annotationJson)

        val container = containerDAO.getCollection(containerName)
        val oldAnnotation = container.find(Document(ANNOTATION_NAME_FIELD, annotationName)).first()
            ?: return Response.status(Response.Status.NOT_FOUND).build()
        val doc = Document(ANNOTATION_NAME_FIELD, annotationName).append(ANNOTATION_FIELD, annotationDocument)
        val updateResult = container.replaceOne(eq(ANNOTATION_NAME_FIELD, annotationName), doc)
        if (!updateResult.wasAcknowledged()) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Failed to update annotation").build()
        }
        val oldFields = JsonLdUtils.extractFields(oldAnnotation[ANNOTATION_FIELD]!!.json)
        updateFieldCount(containerName, newFields, oldFields)
        val annotationData = AnnotationData(
            Instant.now().toEpochMilli(),
            annotationName,
            doc.getEmbedded(listOf(ANNOTATION_FIELD), Document::class.java).toJson(),
            Date.from(Instant.now()),
            Date.from(Instant.now())
        )
        val entity = annotationData.contentWithAssignedId(containerName, annotationName)
        return Response.ok(entity)
            .header("Vary", "Accept")
            .allow("POST", "PUT", "GET", "DELETE", "OPTIONS", "HEAD")
            .link(RESOURCE_LINK, "type")
            .link(ANNOTATION_LINK, "type")
            .tag(eTag)
            .entity(entity)
            .build()
    }

    @Operation(description = "Delete an Annotation")
    @Timed
    @DELETE
    @Path("{containerName}/{annotationName}")
    fun deleteAnnotation(
        @PathParam("containerName") containerName: String,
        @PathParam("annotationName") annotationName: String,
        @Context req: Request,
        @Context context: SecurityContext,
    ): Response {
        log.debug("delete annotation $annotationName in container $containerName")
        context.checkUserHasEditRightsInThisContainer(containerName)

        val eTag = makeAnnotationETag(containerName, annotationName)
        validateETag(req, eTag)

        val container = containerDAO.getCollection(containerName)

        val oldAnnotation = container.find(Document(ANNOTATION_NAME_FIELD, annotationName)).first()
            ?: return Response.status(Response.Status.NOT_FOUND).build()
        val oldFields = JsonLdUtils.extractFields(oldAnnotation[ANNOTATION_FIELD]!!.json)
        updateFieldCount(containerName, emptySet(), oldFields)
        container.findOneAndDelete(Document(ANNOTATION_NAME_FIELD, annotationName))
        return Response.noContent().build()
    }

    private fun jsonParseExceptionMessage(annotationJson: String, e: RuntimeException): String {
        log.error("json parsing error for input:\n{}\n", annotationJson)
        log.error("error:\n{}", e.message)
        return "The given json does not parse: '$annotationJson'"
    }

    private fun setupCollectionMetadata(name: String, label: String, readOnlyForAnonymousUsers: Boolean) {
        val containerMetadataStore = containerDAO.getContainerMetadataCollection()
        val result = containerMetadataStore.replaceOneWithFilter(
            filter = eq(CONTAINER_NAME_FIELD, name),
            replacement = ContainerMetadata(name, label, isReadOnlyForAnonymous = readOnlyForAnonymousUsers),
            replaceOptions = ReplaceOptions().upsert(true)
        )
        log.debug("replace result={}", result)
    }

    private fun AnnotationData.contentWithAssignedId(
        containerName: String, annotationName: String,
    ): Map<String, Any?> {
        val assignedId = uriFactory.annotationURL(containerName, annotationName).toString()
        val jo: MutableMap<String, Any?> =
            Json.createReader(StringReader(content!!)).readObject().toMap().simplify().toMutableMap()
        val originalId = jo["id"]
        jo["id"] = assignedId
        if (originalId != null && originalId != assignedId) {
            jo["via"] = originalId
        }
        return jo.toMap()
    }

    private val paginationStage = Aggregates.limit(configuration.pageSize)
    private fun validateETag(req: Request, eTag: EntityTag) {
        try {
            req.evaluatePreconditions(eTag) ?: throw PreconditionFailedException()
        } catch (e: Throwable) {
            throw BadRequestException(e.message)
        }
    }

    private fun getContainerPage(containerName: String, page: Int, pageSize: Int): ContainerPage? {
        val containerMetadataCollection = containerDAO.getContainerMetadataCollection()
        val metadata = containerMetadataCollection.findOne(Document(CONTAINER_NAME_FIELD, containerName)) ?: return null
        val collection = containerDAO.getCollection(containerName)
        val uri = uriFactory.containerURL(containerName)
        val count = collection.estimatedDocumentCount()
        val annotations = collection.aggregate<Document>(
            Aggregates.match(
                Filters.exists(ANNOTATION_FIELD)
            ), Aggregates.skip(page * pageSize), // start at offset
            paginationStage // return $pageSize documents or less
        ).map { document -> toAnnotationMap(document, containerName) }.toList()

        val lastPage = lastPage(count, pageSize)
        val prevPage = if (page > 0) {
            page - 1
        } else {
            null
        }
        val nextPage = if (lastPage > page) {
            page + 1
        } else {
            null
        }

        return ContainerPage(
            id = uri.toString(),
            label = metadata.label,
            annotations = annotations,
            page = page,
            total = count,
            lastPage = lastPage,
            prevPage = prevPage,
            nextPage = nextPage
        )
    }

    private fun lastPage(count: Long, pageSize: Int) = (count - 1).div(pageSize).toInt()

    private fun toAnnotationMap(a: Document, containerName: String): WebAnnotationAsMap {
        return a.get(ANNOTATION_FIELD, Document::class.java)
            .toMutableMap()
            .apply<MutableMap<String, Any>> {
                put(
                    "id", uriFactory.annotationURL(containerName, a.getString(ANNOTATION_NAME_FIELD))
                )
                remove("@context")
            }
    }

    private fun makeContainerETag(containerName: String): EntityTag =
        EntityTag(abs(containerName.hashCode()).toString(), true)

    private fun updateFieldCount(containerName: String, fieldsAdded: Set<String>, fieldsDeleted: Set<String>) {
        val containerMetadataCollection = containerDAO.getContainerMetadataCollection()
        val containerMetadata: ContainerMetadata =
            containerMetadataCollection.findOne(eq(CONTAINER_NAME_FIELD, containerName)) ?: return
        val fieldCounts = containerMetadata.fieldCounts.toMutableMap()
        for (field in fieldsAdded.filter { f -> !f.contains("@") }) {
            fieldCounts[field] = fieldCounts.getOrDefault(field, 0) + 1
        }
        for (field in fieldsDeleted.filter { f -> !f.contains("@") }) {
            fieldCounts[field] = fieldCounts.getOrDefault(field, 1) - 1
            if (fieldCounts[field] == 0) {
                fieldCounts.remove(field)
            }
        }
        val newContainerMetadata = containerMetadata.copy(fieldCounts = fieldCounts)
        containerMetadataCollection.replaceOne(eq(CONTAINER_NAME_FIELD, containerName), newContainerMetadata)
    }

}
