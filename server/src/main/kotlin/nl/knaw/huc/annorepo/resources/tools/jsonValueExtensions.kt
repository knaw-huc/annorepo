package nl.knaw.huc.annorepo.resources.tools

import jakarta.json.JsonNumber
import jakarta.json.JsonString
import jakarta.json.JsonValue
import nl.knaw.huc.annorepo.api.PropertySet
import nl.knaw.huc.annorepo.api.QueryAsMap

fun JsonValue.toSimpleValue(): Any? {
    return when (valueType) {
        JsonValue.ValueType.NUMBER -> toSimpleNumber()
        JsonValue.ValueType.STRING -> toSimpleString()
        JsonValue.ValueType.TRUE -> true
        JsonValue.ValueType.FALSE -> false
        JsonValue.ValueType.NULL -> null
        JsonValue.ValueType.ARRAY -> toSimpleArray()
        JsonValue.ValueType.OBJECT -> toSimpleMap()
        else -> throw IllegalArgumentException("Invalid JSON value type: $valueType")
    }
}

fun JsonValue.toSimpleMap(): PropertySet {
    val jsonObject = asJsonObject()
    val map = mutableMapOf<String, Any?>()
    jsonObject.forEach { (key, value) -> map[key] = value.toSimpleValue() }
    return map.toMap()
}

fun JsonValue.toSimpleArray(): Array<Any?> =
    asJsonArray()
        .map { it.toSimpleValue() }
        .toTypedArray()

fun JsonValue.toSimpleNumber(): Number {
    val jsonNumber = this as JsonNumber
    return if (jsonNumber.isIntegral) {
        jsonNumber.longValueExact()
    } else {
        jsonNumber.doubleValue()
    }
}

fun JsonValue.toSimpleString(): String {
    val jsonString = this as JsonString
    return jsonString.string
}

fun Map<String, JsonValue>.simplify(): QueryAsMap {
    val newMap = mutableMapOf<String, Any>()
    for (e in entries) {
        val v = e.value
        newMap[e.key] = v.toSimpleValue()!!
    }
    return newMap
}
