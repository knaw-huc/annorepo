package nl.knaw.huc.annorepo.resources

import com.codahale.metrics.annotation.Timed
import com.mongodb.client.MongoClient
import io.swagger.v3.oas.annotations.Operation
import nl.knaw.huc.annorepo.api.ARConst.ANNOTATION_MEDIA_TYPE
import nl.knaw.huc.annorepo.api.ARConst.CONTAINER_METADATA_COLLECTION
import nl.knaw.huc.annorepo.api.AnnotationData
import nl.knaw.huc.annorepo.api.ContainerMetadata
import nl.knaw.huc.annorepo.api.ContainerPage
import nl.knaw.huc.annorepo.api.ContainerSpecs
import nl.knaw.huc.annorepo.api.ResourcePaths
import nl.knaw.huc.annorepo.config.AnnoRepoConfiguration
import nl.knaw.huc.annorepo.service.UriFactory
import org.bson.Document
import org.eclipse.jetty.util.ajax.JSON
import org.litote.kmongo.findOne
import org.litote.kmongo.getCollection
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.*
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.Context
import javax.ws.rs.core.EntityTag
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Request
import javax.ws.rs.core.Response

@Path(ResourcePaths.W3C)
@Produces(ANNOTATION_MEDIA_TYPE)
class W3CResource(
    configuration: AnnoRepoConfiguration,
    client: MongoClient,
) {

    private val log = LoggerFactory.getLogger(javaClass)
    private val uriFactory = UriFactory(configuration)
    private val mdb = client.getDatabase(configuration.databaseName)

    @Operation(description = "Create an Annotation Container")
    @Timed
    @POST
    @Consumes(ANNOTATION_MEDIA_TYPE, MediaType.APPLICATION_JSON)
    fun createContainer(
        containerSpecs: ContainerSpecs,
        @HeaderParam("slug") slug: String?,
    ): Response {
        log.debug("$containerSpecs")
        var name = slug ?: UUID.randomUUID().toString()
        if (mdb.listCollectionNames().contains(name)) {
            log.debug("A container with the suggested name $name already exists, generating a new name.")
            name = UUID.randomUUID().toString()
        }
        mdb.createCollection(name)
        storeCollectionMetadata(containerSpecs.label, name)
        val containerData = getContainerPage(name)
        val uri = uriFactory.containerURL(name)
        val eTag = makeETag(name)
        return Response.created(uri)
            .link("http://www.w3.org/ns/ldp#BasicContainer", "type")
            .link("http://www.w3.org/TR/annotation-protocol", "http://www.w3.org/ns/ldp#constrainedBy")
            .allow("POST", "GET", "DELETE", "OPTIONS", "HEAD")
            .tag(eTag)
            .entity(containerData)
            .build()
    }

    private fun storeCollectionMetadata(label: String, name: String) {
        val cmd = ContainerMetadata(name, label)
        val containerMetadataStore = mdb.getCollection<ContainerMetadata>(CONTAINER_METADATA_COLLECTION)
        containerMetadataStore.insertOne(cmd)
    }

    @Operation(description = "Get an Annotation Container")
    @Timed
    @GET
    @Path("{containerName}")
    fun readContainer(@PathParam("containerName") containerName: String): Response {
        log.debug("read Container $containerName")
        val containerPage = getContainerPage(containerName)
        val eTag = makeETag(containerName)
        return if (containerPage != null) {
            Response.ok(containerPage)
                .link("http://www.w3.org/ns/ldp#BasicContainer", "type")
                .link("http://www.w3.org/TR/annotation-protocol/", "http://www.w3.org/ns/ldp#constrainedBy")
                .allow("POST", "GET", "DELETE", "OPTIONS", "HEAD")
                .tag(eTag)
                .build()
        } else {
            Response.status(Response.Status.NOT_FOUND).entity("Container '$containerName' not found").build()
        }

    }

    private fun makeETag(containerName: String): EntityTag =
        EntityTag(containerName.hashCode().toString())

    @Operation(description = "Delete an empty Annotation Container")
    @Timed
    @DELETE
    @Path("{containerName}")
    fun deleteContainer(
        @PathParam("containerName") containerName: String,
        @Context req: Request
    ): Response {
        log.debug("delete Container $containerName")
        val eTag = makeETag(containerName)
        val valid = req.evaluatePreconditions(eTag)
        val containerPage = getContainerPage(containerName)
        return when {
            valid == null -> {
                Response.status(Response.Status.PRECONDITION_FAILED)
                    .entity("Etag does not match")
                    .build()
            }
            containerPage == null -> {
                Response.status(Response.Status.NOT_FOUND)
                    .entity("Container $containerName was not found.")
                    .build()
            }
            containerPage.total == 0L -> {
                mdb.getCollection(containerName).drop()
                Response.noContent().build()
            }
            else -> {
                Response.status(Response.Status.BAD_REQUEST)
                    .entity("Container $containerName is not empty, all annotations need to be removed from this container first.")
                    .build()
            }
        }
    }

    @Operation(description = "Create an Annotation")
    @Timed
    @POST
    @Path("{containerName}")
    @Consumes(ANNOTATION_MEDIA_TYPE)
    fun createAnnotation(
        @HeaderParam("slug") slug: String?,
        @PathParam("containerName") containerName: String,
        annotationJson: String
    ): Response {
//        log.debug("annotation=\n$annotationJson")
        var name = slug ?: UUID.randomUUID().toString()
        val uri = uriFactory.annotationURL(containerName, name)
        val container = mdb.getCollection(containerName)
        val existingAnnotationDocument = container.find(Document("annotation_name", name)).first()
        if (existingAnnotationDocument != null) {
            log.warn("An annotation with the suggested name $name already exists in container $containerName, generating a new name.")
            name = UUID.randomUUID().toString()
        }
        val annotationDocument = Document.parse(annotationJson)
        val doc = Document("annotation_name", name).append("annotation", annotationDocument)
        val r = container.insertOne(doc).insertedId?.asObjectId()?.value
        val annotationData = AnnotationData(
            r!!.timestamp.toLong(),
            name,
            doc.getEmbedded(listOf("annotation"), Document::class.java).toJson(),
            Date.from(Instant.now()),
            Date.from(Instant.now())
        )
        val entity = withInsertedId(annotationData, containerName, name)
        return Response.created(uri)
            .link("http://www.w3.org/ns/ldp#Resource", "type")
            .link("http://www.w3.org/ns/ldp#Annotation", "type")
            .entity(entity)
            .build()
    }

    @Operation(description = "Get an Annotation")
    @Timed
    @GET
    @Path("{containerName}/{annotationName}")
    @Produces(ANNOTATION_MEDIA_TYPE)
    fun readAnnotation(
        @PathParam("containerName") containerName: String, @PathParam("annotationName") annotationName: String
    ): Response {
        log.debug("read annotation $annotationName in container $containerName")
        val container = mdb.getCollection(containerName)
        val annotationDocument =
            container.find(Document("annotation_name", annotationName))
                .first()
                ?.get("annotation", Document::class.java)
        return if (annotationDocument != null) {
            val annotationData = AnnotationData(
                0L,
                annotationName,
                annotationDocument.toJson(),
                Date.from(Instant.now()),
                Date.from(Instant.now())
            )
            val entity = withInsertedId(annotationData, containerName, annotationName)
            Response.ok(entity)
                .link("http://www.w3.org/ns/ldp#Resource", "type")
                .link("http://www.w3.org/ns/ldp#Annotation", "type")
                .lastModified(annotationData.modified)
                .build()
        } else Response.status(Response.Status.NOT_FOUND).build()
    }

    @Operation(description = "Delete an Annotation")
    @Timed
    @DELETE
    @Path("{containerName}/{annotationName}")
    fun deleteAnnotation(
        @PathParam("containerName") containerName: String, @PathParam("annotationName") annotationName: String
    ) {
        log.debug("delete annotation $annotationName in container $containerName")
        val container = mdb.getCollection(containerName)
        container.findOneAndDelete(Document("annotation_name", annotationName))
    }

    private fun withInsertedId(
        annotationData: AnnotationData, containerName: String, annotationName: String
    ): Any? {
        val content = annotationData.content
        var jo = JSON.parse(content)
        if (jo is HashMap<*, *>) {
            jo = jo.toMutableMap()
            jo["id"] = uriFactory.annotationURL(containerName, annotationName)
        }
        return jo
    }

    private fun getContainerPage(name: String): ContainerPage? {
        val containerMetadataCollection = mdb.getCollection<ContainerMetadata>(CONTAINER_METADATA_COLLECTION)
        val metadata = containerMetadataCollection.findOne(Document("name", name)) ?: return null
        val collection = mdb.getCollection(name)
        val uri = uriFactory.containerURL(name)
        val count = collection.countDocuments()
        return ContainerPage(
            id = uri.toString(),
            label = metadata.label,
            annotations = listOf(),
            startIndex = 0,
            total = count
        )
    }

}
