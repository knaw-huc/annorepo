package nl.knaw.huc.annorepo.grpc

import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.ServerInterceptor
import org.slf4j.LoggerFactory

class GrpcServerInterceptor : ServerInterceptor {
    val log = LoggerFactory.getLogger(javaClass)
    override fun <ReqT, RespT> interceptCall(
        call: ServerCall<ReqT, RespT>,
        headers: Metadata,
        next: ServerCallHandler<ReqT, RespT>
    ): ServerCall.Listener<ReqT> {
        val keyName = "api-key"
        val apiKey = headers.asciiKeyValue(keyName)

        log.debug("keys received: {}", headers.keys())
        log.debug("api-key received = $apiKey")

        return next.startCall(call, headers)
    }

    private fun Metadata.asciiKeyValue(keyName: String): String? {
        val key = Metadata.Key.of(keyName, Metadata.ASCII_STRING_MARSHALLER)
        return get(key)
    }

}
