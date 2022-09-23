package nl.knaw.huc.annorepo.resources

import com.codahale.metrics.annotation.Timed
import com.fasterxml.jackson.databind.ObjectMapper
import com.mongodb.client.MongoClient
import com.mongodb.client.model.Filters
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import nl.knaw.huc.annorepo.api.ARConst
import nl.knaw.huc.annorepo.api.ARConst.ANNOTATION_FIELD
import nl.knaw.huc.annorepo.api.ARConst.ANNOTATION_NAME_FIELD
import nl.knaw.huc.annorepo.api.ARConst.SECURITY_SCHEME_NAME
import nl.knaw.huc.annorepo.api.AnnotationIdentifier
import nl.knaw.huc.annorepo.api.ContainerMetadata
import nl.knaw.huc.annorepo.api.ResourcePaths
import nl.knaw.huc.annorepo.config.AnnoRepoConfiguration
import nl.knaw.huc.annorepo.resources.tools.makeAnnotationETag
import nl.knaw.huc.annorepo.service.JsonLdUtils
import org.bson.Document
import org.litote.kmongo.findOne
import org.litote.kmongo.getCollection
import java.util.*
import javax.annotation.security.PermitAll
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.Context
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.SecurityContext

@Path(ResourcePaths.BATCH)
@Produces(MediaType.APPLICATION_JSON)
@PermitAll
@SecurityRequirement(name = SECURITY_SCHEME_NAME)
class BatchResource(
    private val configuration: AnnoRepoConfiguration,
    private val client: MongoClient,
) {
//    private val log = LoggerFactory.getLogger(javaClass)
//    private val uriFactory = UriFactory(configuration)

    @Operation(description = "Upload annotations in batch to a given container")
    @Timed
    @POST
    @Path("{containerName}/annotations")
    fun postAnnotationsBatch(
        @PathParam("containerName") containerName: String,
        annotations: List<HashMap<String, Any>>,
        @Context context: SecurityContext
    ): Response {
        val annotationIdentifiers = mutableListOf<AnnotationIdentifier>()
        val mdb = client.getDatabase(configuration.databaseName)
        val container = mdb.getCollection(containerName)
        for (i in 0..annotations.size) {
            val annotationName = UUID.randomUUID().toString()
            annotationIdentifiers.add(
                AnnotationIdentifier(
                    containerName = containerName,
                    annotationName = annotationName,
                    etag = makeAnnotationETag(containerName, annotationName).value
                )
            )
        }
        val documents = annotations.mapIndexed { index, annotationMap ->
            val name = annotationIdentifiers[index].annotationName
            Document(ANNOTATION_NAME_FIELD, name).append(ANNOTATION_FIELD, Document(annotationMap))
        }
        container.insertMany(documents)

        val fields = mutableListOf<String>()
        for (annotation in annotations) {
            val annotationJson = ObjectMapper().writeValueAsString(annotation)
            fields.addAll(JsonLdUtils.extractFields(annotationJson).toSet())
        }
        updateFieldCount(containerName, fields, emptySet())
        return Response.ok(annotationIdentifiers).build()
    }

    private fun updateFieldCount(containerName: String, fieldsAdded: List<String>, fieldsDeleted: Set<String>) {
        val mdb = client.getDatabase(configuration.databaseName)
        val containerMetadataCollection = mdb.getCollection<ContainerMetadata>(ARConst.CONTAINER_METADATA_COLLECTION)
        val containerMetadata: ContainerMetadata =
            containerMetadataCollection.findOne(Filters.eq("name", containerName)) ?: return
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
        containerMetadataCollection.replaceOne(Filters.eq("name", containerName), newContainerMetadata)
    }

}