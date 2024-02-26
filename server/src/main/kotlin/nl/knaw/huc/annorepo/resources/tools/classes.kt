package nl.knaw.huc.annorepo.resources.tools

import org.bson.conversions.Bson
import org.bson.types.ObjectId
import nl.knaw.huc.annorepo.api.PropertySet
import nl.knaw.huc.annorepo.api.WebAnnotationAsMap

typealias AggregateStageList = List<Bson>
typealias AnnotationList = List<WebAnnotationAsMap>

data class QueryCacheItem(val queryMap: PropertySet, val aggregateStages: AggregateStageList, val count: Int)

data class RangeParameters(val source: String, val start: Float, val end: Float)

data class MongoDocumentId(val collectionName: String, val objectId: ObjectId)
