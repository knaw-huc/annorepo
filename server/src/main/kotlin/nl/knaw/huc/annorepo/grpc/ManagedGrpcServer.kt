package nl.knaw.huc.annorepo.grpc

import io.dropwizard.lifecycle.Managed
import io.dropwizard.util.Duration
import io.grpc.Server
import org.apache.logging.log4j.kotlin.logger

/**
 * Dropwizard lifecycle management for a gRPC server.
 */
class ManagedGrpcServer(
    private val server: Server,
    private val shutdownTimeout: Duration = Duration.seconds(5)
) : Managed {

    override fun start() {
        logger.info { "Starting gRPC server" }
        server.start()
        logger.info { "gRPC server started on port ${server.port}" }
    }

    override fun stop() {
        logger.info { "Stopping gRPC server on port ${server.port}" }
        val terminatedCleanly = server.shutdown().awaitTermination(shutdownTimeout.quantity, shutdownTimeout.unit)
        if (terminatedCleanly) {
            logger.info { "gRPC server stopped and terminated cleanly." }
        } else {
            logger.info { "gRPC server did not terminate cleanly after $shutdownTimeout" }
            logger.info { "Shutting down gRPC server forcefully." }
            server.shutdownNow()
        }
    }
}