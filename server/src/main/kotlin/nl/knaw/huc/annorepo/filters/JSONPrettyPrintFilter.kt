package nl.knaw.huc.annorepo.filters

import java.io.IOException
import javax.ws.rs.container.ContainerRequestContext
import javax.ws.rs.container.ContainerResponseContext
import javax.ws.rs.container.ContainerResponseFilter
import javax.ws.rs.core.MultivaluedMap
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.ObjectWriter
import com.fasterxml.jackson.jaxrs.cfg.EndpointConfigBase
import com.fasterxml.jackson.jaxrs.cfg.ObjectWriterInjector
import com.fasterxml.jackson.jaxrs.cfg.ObjectWriterModifier

class JSONPrettyPrintFilter : ContainerResponseFilter {
    private class PrettyPrintWriter : ObjectWriterModifier() {
        @Throws(IOException::class)
        override fun modify(
            endpoint: EndpointConfigBase<*>?, responseHeaders: MultivaluedMap<String, Any>,
            valueToWrite: Any, w: ObjectWriter, g: JsonGenerator
        ): ObjectWriter {
            g.useDefaultPrettyPrinter()
            return w
        }
    }

    @Throws(IOException::class)
    override fun filter(requestContext: ContainerRequestContext, responseContext: ContainerResponseContext) {
        ObjectWriterInjector.set(PrettyPrintWriter())
    }
}