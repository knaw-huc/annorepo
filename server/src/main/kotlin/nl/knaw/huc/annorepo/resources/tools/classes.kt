package nl.knaw.huc.annorepo.resources.tools

import org.bson.conversions.Bson

typealias AggregateStageList = List<Bson>
typealias AnnotationList = List<Map<String, Any>>

data class QueryCacheItem(val queryMap: HashMap<*, *>, val aggregateStages: AggregateStageList, val count: Int)

data class RangeParameters(val source: String, val start: Float, val end: Float)
