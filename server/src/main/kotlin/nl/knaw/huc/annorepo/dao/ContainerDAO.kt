package nl.knaw.huc.annorepo.dao

import java.util.SortedMap
import com.mongodb.client.MongoCollection
import org.bson.Document
import nl.knaw.huc.annorepo.api.ContainerMetadata

interface ContainerDAO {
    fun getCollectionStats(containerName: String): Document
    fun getAnnotationFields(containerName: String): SortedMap<String, Int>
    fun getContainerMetadata(containerName: String): ContainerMetadata?
    fun getDistinctValues(containerName: String, field: String): List<Any>
    fun getCollection(containerName: String): MongoCollection<Document>
}