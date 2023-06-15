package nl.knaw.huc.annorepo.resources.tools

import jakarta.ws.rs.core.EntityTag
import kotlin.math.abs

fun makeAnnotationETag(containerName: String, annotationName: String): EntityTag =
    EntityTag(abs("$containerName/$annotationName".hashCode()).toString(), true)

