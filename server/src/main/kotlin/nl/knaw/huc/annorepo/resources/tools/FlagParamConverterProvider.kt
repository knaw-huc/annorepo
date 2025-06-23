package nl.knaw.huc.annorepo.resources.tools

import java.lang.reflect.Type
import jakarta.ws.rs.ext.ParamConverter
import jakarta.ws.rs.ext.ParamConverterProvider
import jakarta.ws.rs.ext.Provider

@Suppress("UNCHECKED_CAST")
@Provider
class FlagParamConverterProvider : ParamConverterProvider {
    override fun <T> getConverter(
        rawType: Class<T?>?,
        genericType: Type?,
        annotations: Array<Annotation?>?
    ): ParamConverter<T?>? =
        if (rawType != Flag::class.java) {
            null
        } else {
            object : ParamConverter<T?> {
                override fun fromString(value: String?): T? = Flag(value) as T

                override fun toString(value: T?): String? = null
            }
        }
}