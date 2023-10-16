package nl.knaw.huc.annorepo.grpc

import io.dropwizard.lifecycle.Managed
import io.dropwizard.util.Duration
import io.grpc.Server
import org.slf4j.LoggerFactory

/**
 * Dropwizard lifecycle management for a gRPC server.
 */
// TODO attach name for logging purposes
class ManagedGrpcServer(
    private val server: Server,
    private val shutdownTimeout: Duration = Duration.seconds(5)
) : Managed {

    override fun start() {
        log.info("Starting gRPC server")
        server.start()
        log.info("gRPC server started on port {}", server.port)
    }

    override fun stop() {
        log.info("Stopping gRPC server on port {}", server.port)
        val terminatedCleanly = server.shutdown().awaitTermination(shutdownTimeout.quantity, shutdownTimeout.unit)
        if (terminatedCleanly) {
            log.info("gRPC server stopped and terminated cleanly.")
        } else {
            log.info("gRPC server did not terminate cleanly after $shutdownTimeout")
            log.info("Shutting down gRPC server forcefully.")
            server.shutdownNow()
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(ManagedGrpcServer::class.java)
    }
}