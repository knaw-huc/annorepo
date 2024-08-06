package nl.knaw.huc.annorepo.dao

import java.util.SortedMap
import com.mongodb.kotlin.client.MongoCollection
import org.bson.Document
import nl.knaw.huc.annorepo.api.AnnotationIdentifier
import nl.knaw.huc.annorepo.api.ContainerMetadata
import nl.knaw.huc.annorepo.api.WebAnnotationAsMap

interface ContainerDAO {
    fun containerExists(containerName: String): Boolean
    fun listCollectionNames(): List<String>
    fun getCollectionStats(containerName: String): Document
    fun getAnnotationFields(containerName: String): SortedMap<String, Int>
    fun createCollection(containerName: String)
    fun getContainerMetadataCollection(): MongoCollection<ContainerMetadata>
    fun getContainerMetadata(containerName: String): ContainerMetadata?
    fun getDistinctValues(containerName: String, field: String): List<Any>
    fun getCollection(containerName: String): MongoCollection<Document>
    fun addAnnotationsInBatch(
        containerName: String,
        annotations: List<WebAnnotationAsMap>
    ): List<AnnotationIdentifier>

}