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
        return extractFields(doc, "")
    }

    private fun extractFields(doc: Map<String, Any>, prefix: String): Set<String> {
        val fields = mutableSetOf<String>()
        for (key in doc.keys) {
            when (val value = doc.getValue(key)) {
                is Map<*, *> -> fields.addAll(extractFields(value as Map<String, Any>, "$prefix$key."))
                is List<*> -> fields.addAll(extractFields(value as List<Any>, "$prefix$key."))
                else -> fields.add("$prefix$key")
            }
        }
        return fields
    }

    private fun extractFields(list: List<Any>, prefix: String): Set<String> {
        val fields = mutableSetOf<String>()
        for (any in list) {
            when (any) {
                is Map<*, *> -> fields.addAll(extractFields(any as Map<String, Any>, prefix))
                is List<*> -> fields.addAll(extractFields(any as List<Any>, prefix))
                else -> fields.add(prefix.trimEnd('.'))
            }
        }
        return fields
    }

    data class JsonLdReport(val isValid: Boolean = false, val invalidFields: List<String> = emptyList())
}