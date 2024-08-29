package nl.knaw.huc.annorepo.dao

import java.util.SortedMap
import java.util.UUID
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
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
    private val MAX_CACHE_SIZE: Long = 100

    private val mdb: MongoDatabase = client.getDatabase(configuration.databaseName)

    private val distinctValuesCache: LoadingCache<String, List<Any>> = CacheBuilder.newBuilder()
        .maximumSize(MAX_CACHE_SIZE)
        .build(CacheLoader.from { _: String -> null })

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
        val annotationsWithViaField = annotations
            .onEach {
                val annotationName = UUID.randomUUID().toString()
                annotationIdentifiers.add(
                    AnnotationIdentifier(
                        containerName = containerName,
                        annotationName = annotationName,
                        etag = makeAnnotationETag(containerName, annotationName).value
                    )
                )
            }
            .map { annotation ->
                annotation.toMutableMap()
                    .apply {
                        val originalId = get("id")
                        if (originalId != null) {
                            put("via", originalId)
                        }
                    }
            }
        val documents = annotationsWithViaField
            .mapIndexed { index, annotationMap ->
                val name = annotationIdentifiers[index].annotationName
                Document(ARConst.ANNOTATION_NAME_FIELD, name)
                    .append(ARConst.ANNOTATION_FIELD, Document(annotationMap))
            }
        container.insertMany(documents)

        val fields: List<String> = annotationsWithViaField
            .map { annotation -> ObjectMapper().writeValueAsString(annotation) }
            .flatMap { jsonString -> JsonLdUtils.extractFields(jsonString) }
            .toSet()
            .sorted()
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

}
