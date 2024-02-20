package nl.knaw.huc.annorepo.dao

import java.util.SortedMap
import java.util.UUID
import com.fasterxml.jackson.databind.ObjectMapper
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
import nl.knaw.huc.annorepo.api.ARConst
import nl.knaw.huc.annorepo.api.AnnotationIdentifier
import nl.knaw.huc.annorepo.api.ContainerMetadata
import nl.knaw.huc.annorepo.api.WebAnnotationAsMap
import nl.knaw.huc.annorepo.config.AnnoRepoConfiguration
import nl.knaw.huc.annorepo.resources.tools.makeAnnotationETag
import nl.knaw.huc.annorepo.resources.tools.toPrimitive
import nl.knaw.huc.annorepo.service.JsonLdUtils

class ARContainerDAO(configuration: AnnoRepoConfiguration, client: MongoClient) : ContainerDAO {
    private val mdb: MongoDatabase = client.getDatabase(configuration.databaseName)
    private val distinctValuesCache: Cache<String, List<Any>> =
        Caffeine.newBuilder().maximumSize(Companion.MAX_CACHE_SIZE).build()

    override fun getCollection(containerName: String): MongoCollection<Document> = mdb.getCollection(containerName)

    override fun getCollectionStats(containerName: String): Document {
        val command = Document("collStats", containerName)
        return mdb.runCommand(command)
    }

    override fun getAnnotationFields(containerName: String): SortedMap<String, Int> =
        getContainerMetadata(containerName)!!.fieldCounts.toSortedMap()

    override fun getContainerMetadataCollection(): MongoCollection<ContainerMetadata> =
        mdb.getCollection<ContainerMetadata>(ARConst.CONTAINER_METADATA_COLLECTION)

    override fun createCollection(containerName: String) {
        mdb.createCollection(containerName)
    }

    override fun listCollectionNames(): List<String> =
        mdb.listCollectionNames().toList()

    override fun getContainerMetadata(containerName: String): ContainerMetadata? =
        getContainerMetadataCollection()
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

    override fun addAnnotationsInBatch(
        containerName: String,
        annotations: List<WebAnnotationAsMap>
    ): List<AnnotationIdentifier> {
        val annotationIdentifiers = mutableListOf<AnnotationIdentifier>()
        val container = getCollection(containerName)
        for (i in annotations.indices) {
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
            Document(ARConst.ANNOTATION_NAME_FIELD, name).append(ARConst.ANNOTATION_FIELD, Document(annotationMap))
        }
        container.insertMany(documents)

        val fields = mutableListOf<String>()
        for (annotation in annotations) {
            val annotationJson = ObjectMapper().writeValueAsString(annotation)
            fields.addAll(JsonLdUtils.extractFields(annotationJson).toSet())
        }
        updateFieldCount(containerName, fields, emptySet())
        return annotationIdentifiers
    }

    override fun containerExists(containerName: String): Boolean = mdb.listCollectionNames().contains(containerName)

    private fun updateFieldCount(containerName: String, fieldsAdded: List<String>, fieldsDeleted: Set<String>) {
        val containerMetadataCollection = getContainerMetadataCollection()
        val containerMetadata: ContainerMetadata =
            getContainerMetadata(containerName)!!
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

    companion object {
        private const val MAX_CACHE_SIZE: Long = 100
    }

}
