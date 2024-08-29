package nl.knaw.huc.annorepo.grpc

class SayHelloService : HelloServiceGrpcKt.HelloServiceCoroutineImplBase() {
    override suspend fun sayHello(request: SayHelloRequest): SayHelloResponse =
        sayHelloResponse { message = "Hello ${request.name}" }
}
