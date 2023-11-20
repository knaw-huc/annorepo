package nl.knaw.huc.annorepo.config

import com.fasterxml.jackson.annotation.JsonProperty
import io.dropwizard.core.setup.Environment
import io.dropwizard.util.Duration
import io.grpc.ServerBuilder

class GrpcFactory {
    @JsonProperty("port")
    var port: Int = 8000

    @JsonProperty("hostname")
    var hostName: String = "localhost"

    @JsonProperty("shutdownPeriod")
    var shutdownPeriod: Duration = Duration.seconds(5)

    fun builder(environment: Environment): ServerBuilder<*> {
        val originBuilder: ServerBuilder<*> = ServerBuilder.forPort(port)
        return DropwizardServerBuilder(environment, originBuilder, shutdownPeriod)
    }
}
