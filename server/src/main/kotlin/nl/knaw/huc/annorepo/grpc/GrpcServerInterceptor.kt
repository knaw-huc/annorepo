package nl.knaw.huc.annorepo.grpc

import jakarta.inject.Singleton
import io.grpc.Context
import io.grpc.Contexts
import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.ServerInterceptor
import org.slf4j.LoggerFactory

@Singleton
class GrpcServerInterceptor : ServerInterceptor {
    val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val HEADERS_KEY = "headers"
        val HEADERS_VALUE: Context.Key<Map<String, String>> = Context.key(HEADERS_KEY)
    }

    override fun <ReqT, RespT> interceptCall(
        call: ServerCall<ReqT, RespT>,
        headers: Metadata,
        next: ServerCallHandler<ReqT, RespT>
    ): ServerCall.Listener<ReqT> {
        var ctx = Context.current()
        ctx = ctx.withValue(HEADERS_VALUE, extractHeaders(headers))
        return Contexts.interceptCall(ctx, call, headers, next)
    }

    private fun extractHeaders(headers: Metadata?) =
        headers?.keys()!!.associateWith {
            headers.get(Metadata.Key.of(it, Metadata.ASCII_STRING_MARSHALLER)) as String
        }
}
