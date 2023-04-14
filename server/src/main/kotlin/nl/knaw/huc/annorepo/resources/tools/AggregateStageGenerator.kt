package nl.knaw.huc.annorepo.resources.tools

import jakarta.json.JsonNumber
import jakarta.json.JsonString
import jakarta.ws.rs.BadRequestException
import com.mongodb.client.model.Aggregates
import com.mongodb.client.model.Filters
import org.bson.conversions.Bson
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import nl.knaw.huc.annorepo.config.AnnoRepoConfiguration

class AggregateStageGenerator(val configuration: AnnoRepoConfiguration) {
    val log: Logger = LoggerFactory.getLogger(this::class.java)

    fun generateStage(key: Any, value: Any): Bson =
        when (key) {
            !is String -> throw BadRequestException("Unexpected field: '$key' ; query root fields should be strings")
            WITHIN_RANGE -> withinRangeStage(value)
            OVERLAPPING_WITH_RANGE -> overlappingWithRangeStage(value)
            else -> {
                if (key.startsWith(":")) {
                    throw BadRequestException("Unknown query function: '$key'")
                } else {
                    log.info("key={}, value={} ({})", key, value, value.javaClass)
                    fieldMatchStage(key, value)
                }
            }
        }

    @Suppress("UNCHECKED_CAST")
    private fun fieldMatchStage(key: String, value: Any): Bson =
        when (value) {
            is Map<*, *> -> specialFieldMatchStage(key, value as Map<String, Any>)
            else -> Aggregates.match(Filters.eq("$ANNOTATION_FIELD_PREFIX$key", value))
        }

    private fun specialFieldMatchStage(field: String, value: Map<String, Any>): Bson =
        Filters.and(value.map { (k, v) ->
            return when (k) {
                IS_NOT_IN -> {
                    log.info("v={} ({})", v, v.javaClass)
                    Aggregates.match(
                        Filters.nin("$ANNOTATION_FIELD_PREFIX$field", *(v as Array<*>))
                    )
                }

                IS_IN -> Aggregates.match(
                    Filters.`in`("$ANNOTATION_FIELD_PREFIX$field", *(v as Array<*>))
                )

                IS_GREATER -> Aggregates.match(
                    Filters.gt("$ANNOTATION_FIELD_PREFIX$field", v)
                )

                IS_GREATER_OR_EQUAL -> Aggregates.match(
                    Filters.gte("$ANNOTATION_FIELD_PREFIX$field", v)
                )

                IS_LESS -> Aggregates.match(
                    Filters.lt("$ANNOTATION_FIELD_PREFIX$field", v)
                )

                IS_LESS_OR_EQUAL -> Aggregates.match(
                    Filters.lte("$ANNOTATION_FIELD_PREFIX$field", v)
                )

                IS_EQUAL_TO -> Aggregates.match(
                    Filters.eq("$ANNOTATION_FIELD_PREFIX$field", v)
                )

                IS_NOT -> Aggregates.match(
                    Filters.ne("$ANNOTATION_FIELD_PREFIX$field", v)
                )

                else -> throw BadRequestException("unknown selector '$k'")
            }
        })

    private fun overlappingWithRangeStage(rawParameters: Any): Bson =
        when (rawParameters) {
            is Map<*, *> -> {
                val rangeParameters = rangeParameters(rawParameters)
                Aggregates.match(
                    Filters.and(
                        Filters.eq("${ANNOTATION_FIELD_PREFIX}target.source", rangeParameters.source),
                        Filters.eq("${ANNOTATION_FIELD_PREFIX}target.selector.type", configuration.rangeSelectorType),
                        Filters.lt("${ANNOTATION_FIELD_PREFIX}target.selector.start", rangeParameters.end),
                        Filters.gt("${ANNOTATION_FIELD_PREFIX}target.selector.end", rangeParameters.start),
                    )
                )
            }

            else -> throw BadRequestException("invalid parameter: $rawParameters")
        }

    private fun withinRangeStage(rawParameters: Any): Bson =
        when (rawParameters) {
            is Map<*, *> -> {
                val rangeParameters = rangeParameters(rawParameters)
                Aggregates.match(
                    Filters.and(
                        Filters.eq("${ANNOTATION_FIELD_PREFIX}target.source", rangeParameters.source),
                        Filters.eq("${ANNOTATION_FIELD_PREFIX}target.selector.type", configuration.rangeSelectorType),
                        Filters.gte("${ANNOTATION_FIELD_PREFIX}target.selector.start", rangeParameters.start),
                        Filters.lte("${ANNOTATION_FIELD_PREFIX}target.selector.end", rangeParameters.end),
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
