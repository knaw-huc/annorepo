package nl.knaw.huc.annorepo.dao

import java.util.SortedMap
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters
import org.bson.BsonValue
import org.bson.Document
import org.litote.kmongo.findOne
import org.litote.kmongo.getCollection
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import nl.knaw.huc.annorepo.api.ARConst
import nl.knaw.huc.annorepo.api.ContainerMetadata
import nl.knaw.huc.annorepo.config.AnnoRepoConfiguration
import nl.knaw.huc.annorepo.resources.tools.toPrimitive

class ARContainerDAO(configuration: AnnoRepoConfiguration, client: MongoClient) : ContainerDAO {
    private val MAX_CACHE_SIZE: Long = 100
    val log: Logger = LoggerFactory.getLogger(ARContainerDAO::class.java)

    private val mdb: MongoDatabase = client.getDatabase(configuration.databaseName)
    private val distinctValuesCache: Cache<String, List<Any>> =
        Caffeine.newBuilder().maximumSize(MAX_CACHE_SIZE).build()

    override fun getCollection(containerName: String): MongoCollection<Document> = mdb.getCollection(containerName)

    override fun getCollectionStats(containerName: String): Document {
        val command = Document("collStats", containerName)
        return mdb.runCommand(command)
    }

    override fun getAnnotationFields(containerName: String): SortedMap<String, Int> =
        getContainerMetadata(containerName)!!.fieldCounts.toSortedMap()

    override fun getContainerMetadata(containerName: String): ContainerMetadata? =
        mdb.getCollection<ContainerMetadata>(ARConst.CONTAINER_METADATA_COLLECTION)
            .findOne(Filters.eq(ARConst.CONTAINER_NAME_FIELD, containerName))

    override fun getDistinctValues(containerName: String, field: String): List<Any> {
        val size = getCollectionStats(containerName)["size"]
        val cacheKey = "$containerName:$size:$field"
        return distinctValuesCache.get(cacheKey) {
            getCollection(containerName)
                .distinct("${ARConst.ANNOTATION_FIELD}.$field", BsonValue::class.java)
                .map { it.toPrimitive()!! }
                .toList()
        }
    }
}
