package nl.knaw.huc.annorepo.resources.tools

import jakarta.ws.rs.core.EntityTag
import kotlin.math.abs
import kotlin.math.log2
import kotlin.math.pow
import com.mongodb.kotlin.client.MongoCollection
import org.bson.BsonType
import org.bson.BsonValue
import org.bson.Document
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import nl.knaw.huc.annorepo.api.ARConst.ANNOTATION_NAME_FIELD
import nl.knaw.huc.annorepo.api.IndexType

fun makeAnnotationETag(containerName: String, annotationName: String): EntityTag =
    EntityTag(abs("$containerName/$annotationName".hashCode()).toString(), true)

val Long.formatAsSize: String
    get() = log2(coerceAtLeast(1).toDouble()).toInt().div(10).let {
        val precision = when (it) {
            0 -> 0; 1 -> 1; else -> 2
        }
        val prefix = arrayOf("", "K", "M", "G", "T", "P", "E", "Z", "Y")
        String.format("%.${precision}f ${prefix[it]}B", toDouble() / 2.0.pow(it * 10.0))
    }

fun BsonValue.toPrimitive(): Any? =
    when (this.bsonType) {
        BsonType.BOOLEAN -> this.asBoolean().value
        BsonType.DATE_TIME -> this.asDateTime().value
        BsonType.DECIMAL128 -> this.asDecimal128().value
        BsonType.DOUBLE -> this.asDouble().value
        BsonType.INT32 -> this.asInt32().value
        BsonType.INT64 -> this.asInt64().value
        BsonType.STRING -> this.asString().value
        BsonType.TIMESTAMP -> this.asTimestamp().value
        else -> this
    }

val ANNOTATION_NAME_INDEX_NAME = "${ANNOTATION_NAME_FIELD}_${IndexType.HASHED.name.lowercase()}"

val log: Logger = LoggerFactory.getLogger("functions")
fun MongoCollection<Document>.hasAnnotationNameIndex(): Boolean =
    listIndexes()
        .toList()
        .map { it["name"] }
        .contains(ANNOTATION_NAME_INDEX_NAME)

fun annotationCollectionLink(id: String, collectionLabel: String? = null): Map<String, String> {
    val collectionMap = mutableMapOf(
        "id" to id,
        "type" to "AnnotationCollection"
    )
    if (collectionLabel != null) {
        collectionMap["label"] = collectionLabel
    }
    return collectionMap
}
