@file:Suppress("UNCHECKED_CAST")

package nl.knaw.huc.annorepo.service

import org.bson.Document

object JsonLdUtils {

    fun checkFieldContext(jsonld: String): JsonLdReport {
        val isValid = false
        return JsonLdReport(isValid)
    }

    fun extractFields(jsonld: String): Set<String> {
        val doc = Document.parse(jsonld).toMap()
        return extractFields(doc, "").toSet()
    }

    private fun extractFields(doc: Map<String, Any>, prefix: String): List<String> =
        doc.keys.flatMap { key ->
            when (val value = doc.getValue(key)) {
                is Map<*, *> -> extractFields(value as Map<String, Any>, "$prefix$key.")
                is List<*> -> extractFields(value as List<Any>, "$prefix$key.")
                else -> listOf("$prefix$key")
            }
        }

    private fun extractFields(list: List<Any>, prefix: String): List<String> =
        list.flatMap { any ->
            when (any) {
                is Map<*, *> -> extractFields(any as Map<String, Any>, prefix)
                is List<*> -> extractFields(any as List<Any>, prefix)
                else -> listOf(prefix.trimEnd('.'))
            }
        }

    data class JsonLdReport(val isValid: Boolean = false, val invalidFields: List<String> = emptyList())
}