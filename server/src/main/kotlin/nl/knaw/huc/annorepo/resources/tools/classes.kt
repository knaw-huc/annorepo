package nl.knaw.huc.annorepo.resources.tools

import org.bson.conversions.Bson
import org.bson.types.ObjectId

typealias AggregateStageList = List<Bson>
typealias AnnotationList = List<Map<String, Any>>

data class QueryCacheItem(val queryMap: Map<String, Any?>, val aggregateStages: AggregateStageList, val count: Int)

data class RangeParameters(val source: String, val start: Float, val end: Float)

data class MongoDocumentId(val collectionName: String, val objectId: ObjectId)
