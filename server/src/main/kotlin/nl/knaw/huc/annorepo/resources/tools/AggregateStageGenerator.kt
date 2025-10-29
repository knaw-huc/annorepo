package nl.knaw.huc.annorepo.resources.tools

import jakarta.json.JsonNumber
import jakarta.json.JsonString
import jakarta.ws.rs.BadRequestException
import com.mongodb.client.model.Aggregates.match
import com.mongodb.client.model.Filters
import org.apache.logging.log4j.kotlin.logger
import org.bson.conversions.Bson
import nl.knaw.huc.annorepo.config.AnnoRepoConfiguration

class AggregateStageGenerator(val configuration: AnnoRepoConfiguration) {

    fun generateStage(key: Any, value: Any): Bson =
        when (key) {
            !is String -> throw BadRequestException("Unexpected field: '$key' ; query root fields should be strings")
            OR -> match(either(value))
            AND -> match(all(value))
            WITHIN_RANGE -> withinRangeStage(value)
            OVERLAPPING_WITH_RANGE -> overlappingWithRangeStage(value)
            else -> {
                if (key.startsWith(":")) {
                    throw BadRequestException("Unknown query function: '$key'")
                } else {
                    logger.debug { "key=$key, value=$value (${value.javaClass})" }
                    match(fieldMatchStage(key, value))
                }
            }
        }

    private fun either(value: Any): Bson =
        try {
            val valueAsList = (value as Array<*>).toList()
            Filters.or(valueAsList.flatMap { it.asAggregate() })
        } catch (e: ClassCastException) {
            throw BadRequestException("The value for $OR must be a list")
        }

    private fun all(value: Any): Bson =
        try {
            val valueAsList = (value as Array<*>).toList()
            Filters.and(valueAsList.flatMap { it.asAggregate() })
        } catch (e: ClassCastException) {
            throw BadRequestException("The value for $AND must be a list")
        }

    private fun Any?.asAggregate(): List<Bson> =
        when (this) {
            is Map<*, *> -> map { (key, value) ->
                when (key) {
                    OR -> either(value!!)
                    AND -> all(value!!)
                    WITHIN_RANGE -> withinRangeStage(value!!)
                    OVERLAPPING_WITH_RANGE -> overlappingWithRangeStage(value!!)
                    else -> fieldMatchStage(key!! as String, value!!)
                }
            }

            else -> throw BadRequestException("Unexpected value: expected $this to be a field: value subquery")
        }

    @Suppress("UNCHECKED_CAST")
    private fun fieldMatchStage(key: String, value: Any): Bson =
        when (value) {
            is Map<*, *> -> specialFieldMatchStage(key, value as Map<String, Any>)
            else -> Filters.eq("$ANNOTATION_FIELD_PREFIX$key", value)
        }

    private fun specialFieldMatchStage(field: String, value: Map<String, Any>): Bson {
        val filters = value.map { (k, v) ->
            when (k) {
                IS_NOT_IN ->
                    try {
                        val valueAsList = (v as Array<*>).toList()
                        Filters.nin("$ANNOTATION_FIELD_PREFIX$field", valueAsList)
                    } catch (e: ClassCastException) {
                        throw BadRequestException("$IS_NOT_IN parameter must be a list")
                    }

                IS_IN ->
                    try {
                        val valueAsList = (v as Array<*>).toList()
                        Filters.`in`("$ANNOTATION_FIELD_PREFIX$field", valueAsList)
                    } catch (e: ClassCastException) {
                        throw BadRequestException("$IS_IN parameter must be a list")
                    }

                IS_GREATER ->
                    Filters.gt("$ANNOTATION_FIELD_PREFIX$field", v)

                IS_GREATER_OR_EQUAL ->
                    Filters.gte("$ANNOTATION_FIELD_PREFIX$field", v)

                IS_LESS ->
                    Filters.lt("$ANNOTATION_FIELD_PREFIX$field", v)

                IS_LESS_OR_EQUAL ->
                    Filters.lte("$ANNOTATION_FIELD_PREFIX$field", v)

                IS_EQUAL_TO ->
                    Filters.eq("$ANNOTATION_FIELD_PREFIX$field", v)

                IS_NOT ->
                    Filters.ne("$ANNOTATION_FIELD_PREFIX$field", v)

                else -> throw BadRequestException("unknown selector '$k'")
            }
        }
        return if (filters.size == 1) {
            filters.first()
        } else {
            Filters.and(filters)
        }
    }

    private fun overlappingWithRangeStage(rawParameters: Any): Bson =
        when (rawParameters) {
            is Map<*, *> -> {
                val rangeParameters = rangeParameters(rawParameters)
                match(
                    Filters.elemMatch(
                        "${ANNOTATION_FIELD_PREFIX}target",
                        Filters.and(
                            Filters.eq("type", configuration.rangeTargetType),
                            Filters.eq("source", rangeParameters.source),
                            Filters.eq("selector.type", configuration.rangeSelectorType),
                            Filters.lte("selector.start", rangeParameters.end),
                            Filters.gte("selector.end", rangeParameters.start),
                        )
                    )
                )
            }

            else -> throw BadRequestException("invalid parameter: $rawParameters")
        }

    private fun withinRangeStage(rawParameters: Any): Bson =
        when (rawParameters) {
            is Map<*, *> -> {
                val rangeParameters = rangeParameters(rawParameters)
                match(
                    Filters.elemMatch(
                        "${ANNOTATION_FIELD_PREFIX}target",
                        Filters.and(
                            Filters.eq("type", configuration.rangeTargetType),
                            Filters.eq("source", rangeParameters.source),
                            Filters.eq("selector.type", configuration.rangeSelectorType),//TextPositionSelector
                            Filters.gte("selector.start", rangeParameters.start),
                            Filters.lte("selector.end", rangeParameters.end),
                        )
                    )
                )
            }

            else -> throw BadRequestException("invalid parameter: $rawParameters")
        }

    private fun rangeParameters(v: Map<*, *>): RangeParameters {
        val source: String = v.stringValue("source")
        val start: Float = v.floatValue("start")
        val end: Float = v.floatValue("end")
        return RangeParameters(source, start, end)
    }

    private fun Map<*, *>.floatValue(key: String): Float {
        if (!containsKey(key)) {
            throw BadRequestException("missing float parameter '$key'")
        }
        return when (val startValue = get(key)) {
            is Number -> startValue.toFloat()
            is JsonNumber -> startValue.numberValue().toFloat()
            else -> throw BadRequestException("parameter '$key' should be a float, but is ${startValue?.javaClass}")
        }
    }

    private fun Map<*, *>.stringValue(key: String): String {
        if (!containsKey(key)) {
            throw BadRequestException("missing string parameter '$key'")
        }
        return when (val sourceValue = get(key)) {
            is String -> sourceValue
            is JsonString -> sourceValue.string
            else -> throw BadRequestException("parameter '$key' should be a string, but is ${sourceValue?.javaClass}")
        }
    }
}