package nl.knaw.huc.annorepo.resources

import jakarta.annotation.security.PermitAll
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.Context
import jakarta.ws.rs.core.SecurityContext
import com.codahale.metrics.annotation.Timed
import com.mongodb.client.MongoClient
import com.mongodb.client.model.Filters.exists
import io.swagger.v3.oas.annotations.Hidden
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import org.bson.Document
import org.litote.kmongo.aggregate
import org.litote.kmongo.match
import nl.knaw.huc.annorepo.api.ARConst.ANNOTATION_MEDIA_TYPE
import nl.knaw.huc.annorepo.api.ARConst.ANNOTATION_NAME_FIELD
import nl.knaw.huc.annorepo.api.ARConst.CONTAINER_METADATA_COLLECTION
import nl.knaw.huc.annorepo.api.ARConst.SECURITY_SCHEME_NAME
import nl.knaw.huc.annorepo.api.ResourcePaths
import nl.knaw.huc.annorepo.config.AnnoRepoConfiguration
import nl.knaw.huc.annorepo.service.UriFactory

@Hidden
@Path(ResourcePaths.LIST)
@Produces(ANNOTATION_MEDIA_TYPE)
@PermitAll
@SecurityRequirement(name = SECURITY_SCHEME_NAME)
class ListResource(
    private val configuration: AnnoRepoConfiguration,
    client: MongoClient,
    private val uriFactory: UriFactory
) {
    private val mdb = client.getDatabase(configuration.databaseName)

    @Operation(description = "Get a list of all the container URLs")
    @Timed
    @GET
    @Path("containers")
    fun getContainerURLs(@Context context: SecurityContext): List<String> =
        mdb.listCollectionNames()
            .filter { it != CONTAINER_METADATA_COLLECTION }
            .map { uriFactory.containerURL(it).toString() }
            .sorted()
            .toList()

    @Operation(description = "Get a list of all the annotation URLs")
    @Timed
    @GET
    @Path("{containerName}/annotations")
    fun getAnnotationURLs(
        @PathParam("containerName") containerName: String,
        @QueryParam("start") offset: Int = 0,
        @Context context: SecurityContext,
    ): List<String> =
        mdb.getCollection(containerName).aggregate<Document>(
            match(exists(ANNOTATION_NAME_FIELD)),
        )
            .map { d -> uriFactory.annotationURL(containerName, d.getString(ANNOTATION_NAME_FIELD)).toString() }
            .toList()
            .sorted()
            .subList(fromIndex = offset, toIndex = offset + configuration.pageSize)

}