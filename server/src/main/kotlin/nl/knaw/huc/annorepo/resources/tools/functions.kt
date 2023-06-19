package nl.knaw.huc.annorepo.resources.tools

import jakarta.ws.rs.core.EntityTag
import kotlin.math.abs
import kotlin.math.log2
import kotlin.math.pow

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