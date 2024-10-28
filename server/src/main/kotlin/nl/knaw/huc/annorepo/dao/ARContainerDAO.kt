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
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.client.result.UpdateResult
import org.bson.BsonValue
import org.bson.Document
import org.litote.kmongo.findOne
import org.litote.kmongo.getCollection
import org.litote.kmongo.replaceOneWithFilter
import nl.knaw.huc.annorepo.api.ARConst
import nl.knaw.huc.annorepo.api.ARConst.CONTAINER_NAME_FIELD
import nl.knaw.huc.annorepo.api.AnnotationIdentifier
import nl.knaw.huc.annorepo.api.ContainerMetadata
import nl.knaw.huc.annorepo.api.IndexConfig
import nl.knaw.huc.annorepo.api.IndexFields
import nl.knaw.huc.annorepo.api.IndexType
import nl.knaw.huc.annorepo.api.WebAnnotationAsMap
import nl.knaw.huc.annorepo.config.AnnoRepoConfiguration
import nl.knaw.huc.annorepo.resources.tools.makeAnnotationETag
import nl.knaw.huc.annorepo.resources.tools.toPrimitive
import nl.knaw.huc.annorepo.service.JsonLdUtils
import nl.knaw.huc.annorepo.service.UriFactory

class ARContainerDAO(
    configuration: AnnoRepoConfiguration, client: MongoClient, val uriFactory: UriFactory
) : ContainerDAO {

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
        getContainerMetadata(containerName)?.fieldCounts?.toSortedMap() ?: emptyMap<String, Int>().toSortedMap()

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

    override fun updateContainerMetadata(
        containerName: String,
        containerMetadata: ContainerMetadata,
        upsert: Boolean
    ): UpdateResult =
        getContainerMetadataCollection()
            .replaceOneWithFilter(
                filter = eq(CONTAINER_NAME_FIELD, containerName),
                replacement = containerMetadata,
                replaceOptions = ReplaceOptions().upsert(true)
            )

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

    override fun getContainerIndexDefinition(containerName: String, indexId: String): Any {
        val mongoIndexName = mongoIndexName(containerName, indexId)
        return indexConfig(containerName, mongoIndexName, indexId)
    }

    override fun indexConfig(
        containerName: String,
        mongoIndexName: String,
        indexId: String
    ): IndexConfig {
        val nameParts = mongoIndexName
            .split("_")
            .chunked(2)
            .map {
                val fieldName = it[0]
                val indexType = when (it[1]) {
                    IndexType.HASHED.mongoSuffix -> IndexType.HASHED
                    IndexType.ASCENDING.mongoSuffix -> IndexType.ASCENDING
                    IndexType.DESCENDING.mongoSuffix -> IndexType.DESCENDING
                    IndexType.TEXT.mongoSuffix -> IndexType.TEXT
                    else -> throw Exception("unexpected index type: ${it[1]} in ${it[0]}")
                }
                Pair(fieldName, indexType)
            }
        return IndexConfig(
            id = indexId,
            url = uriFactory.containerIndexURL(containerName, indexId),
            indexFields = nameParts.map { IndexFields(field = it.first, type = it.second) })
    }

    override fun dropContainerIndex(containerName: String, indexId: String) {
        val mongoIndexName = mongoIndexName(containerName, indexId)
        getCollection(containerName).dropIndex(mongoIndexName)
    }

    override fun containerExists(containerName: String): Boolean = mdb.listCollectionNames().contains(containerName)

    private fun updateFieldCount(containerName: String, fieldsAdded: List<String>, fieldsDeleted: Set<String>) {
        val containerMetadataCollection = getContainerMetadataCollection()
        val containerMetadata: ContainerMetadata = getContainerMetadata(containerName)
            ?: throw RuntimeException("No container metadata found for container $containerName")
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

    private fun mongoIndexName(containerName: String, indexId: String): String {
        val containerMetadata = getContainerMetadata(containerName)
            ?: throw RuntimeException("No metadata found for container $containerName")
        val mongoIndexName = containerMetadata.indexMap[indexId]
            ?: throw RuntimeException("indexId $indexId not found in container metadata for container $containerName")
        return mongoIndexName
    }

    companion object {
        private const val MAX_CACHE_SIZE: Long = 100
    }

}
