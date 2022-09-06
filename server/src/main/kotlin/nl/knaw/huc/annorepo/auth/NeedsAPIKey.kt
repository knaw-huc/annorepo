package nl.knaw.huc.annorepo.auth

import javax.ws.rs.NameBinding
import kotlin.annotation.AnnotationTarget.ANNOTATION_CLASS
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.FUNCTION
import kotlin.annotation.AnnotationTarget.PROPERTY_GETTER
import kotlin.annotation.AnnotationTarget.PROPERTY_SETTER

@Target(
    ANNOTATION_CLASS,
    CLASS,
    FUNCTION,
    PROPERTY_GETTER,
    PROPERTY_SETTER
)
@Retention(AnnotationRetention.RUNTIME)
@NameBinding
annotation class NeedsAPIKey