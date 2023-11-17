package nl.knaw.huc.annorepo.grpc

import jakarta.inject.Singleton
import io.grpc.Context
import io.grpc.Contexts
import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.ServerInterceptor
import io.grpc.Status
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import nl.knaw.huc.annorepo.api.GRPC_METADATA_KEY_API_KEY
import nl.knaw.huc.annorepo.api.GRPC_METADATA_KEY_CONTAINER_NAME
import nl.knaw.huc.annorepo.api.Role
import nl.knaw.huc.annorepo.dao.ContainerUserDAO
import nl.knaw.huc.annorepo.dao.UserDAO

@Singleton
class GrpcServerInterceptor(
    private val userDAO: UserDAO,
    private val containerUserDAO: ContainerUserDAO
) : ServerInterceptor {
    val log: Logger = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val HEADERS_KEY = "headers"
        val HEADERS_VALUE: Context.Key<Map<String, String>> = Context.key(HEADERS_KEY)
        private val rolesWithWriteAccess = setOf(Role.ADMIN, Role.EDITOR, Role.ROOT)
    }

    override fun <ReqT, RespT> interceptCall(
        servercall: ServerCall<ReqT, RespT>,
        headers: Metadata,
        next: ServerCallHandler<ReqT, RespT>
    ): ServerCall.Listener<ReqT> {
        val headersMap = extractHeaders(headers)
        val status: Status = when {
            !headersMap.containsKey(GRPC_METADATA_KEY_CONTAINER_NAME) -> {
                Status.FAILED_PRECONDITION.withDescription("key $GRPC_METADATA_KEY_CONTAINER_NAME not found in headers")
            }

            !headersMap.containsKey(GRPC_METADATA_KEY_API_KEY) -> {
                Status.UNAUTHENTICATED.withDescription("key $GRPC_METADATA_KEY_API_KEY not found in headers")
            }

            writeAccessDeniedForContainer(headersMap) -> {
                Status.PERMISSION_DENIED.withDescription("no write access to the given container with the given api-key")
            }

            else -> {
                Status.OK
            }
        }

        if (status === Status.OK) {
            val ctx = Context.current().withValue(HEADERS_VALUE, headersMap)
            return Contexts.interceptCall(ctx, servercall, headers, next)
        }
        servercall.close(status, Metadata())
        return object : ServerCall.Listener<ReqT>() {}
    }

    private fun extractHeaders(headers: Metadata): Map<String, String> =
        headers.keys()?.associateWith {
            headers.get(Metadata.Key.of(it, Metadata.ASCII_STRING_MARSHALLER)) as String
        } ?: mapOf()

    private fun writeAccessDeniedForContainer(headersMap: Map<String, String>): Boolean {
        val apiKey = headersMap[GRPC_METADATA_KEY_API_KEY]
        val containerName = headersMap[GRPC_METADATA_KEY_CONTAINER_NAME] ?: "no-container-name"
        val userName = userDAO.userForApiKey(apiKey)?.name ?: "no-user"
        val role = containerUserDAO.getUserRole(containerName, userName)
        return !rolesWithWriteAccess.contains(role)
    }

}
