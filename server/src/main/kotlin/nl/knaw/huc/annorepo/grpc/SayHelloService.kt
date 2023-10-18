package nl.knaw.huc.annorepo.grpc

import org.slf4j.LoggerFactory

class SayHelloService : HelloServiceGrpcKt.HelloServiceCoroutineImplBase() {
    val log = LoggerFactory.getLogger(SayHelloService::class.java)
    override suspend fun sayHello(request: SayHelloRequest): SayHelloResponse =
        sayHelloResponse { message = "Hello ${request.name}" }
}
