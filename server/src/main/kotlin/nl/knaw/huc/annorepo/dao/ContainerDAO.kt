package nl.knaw.huc.annorepo.dao

import java.util.SortedMap
import com.mongodb.client.MongoCollection
import org.bson.Document
import nl.knaw.huc.annorepo.api.AnnotationIdentifier
import nl.knaw.huc.annorepo.api.ContainerMetadata
import nl.knaw.huc.annorepo.api.WebAnnotationAsMap

interface ContainerDAO {
    fun getCollectionStats(containerName: String): Document
    fun getAnnotationFields(containerName: String): SortedMap<String, Int>
    fun getContainerMetadata(containerName: String): ContainerMetadata?
    fun getDistinctValues(containerName: String, field: String): List<Any>
    fun getCollection(containerName: String): MongoCollection<Document>
    fun addAnnotationsInBatch(
        containerName: String,
        annotations: List<WebAnnotationAsMap>
    ): List<AnnotationIdentifier>

    fun containerExists(containerName: String): Boolean

    fun getContainerMetadataCollection(): MongoCollection<ContainerMetadata>
    fun createCollection(containerName: String)
    fun listCollectionNames(): List<String>
}