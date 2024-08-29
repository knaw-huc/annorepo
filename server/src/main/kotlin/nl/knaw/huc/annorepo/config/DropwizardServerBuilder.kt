package nl.knaw.huc.annorepo.config

import java.io.File
import java.util.concurrent.Executor
import io.dropwizard.core.setup.Environment
import io.dropwizard.util.Duration
import io.grpc.BindableService
import io.grpc.CompressorRegistry
import io.grpc.DecompressorRegistry
import io.grpc.HandlerRegistry
import io.grpc.Server
import io.grpc.ServerBuilder
import io.grpc.ServerInterceptor
import io.grpc.ServerServiceDefinition
import io.grpc.ServerStreamTracer.Factory
import io.grpc.ServerTransportFilter
import nl.knaw.huc.annorepo.grpc.ManagedGrpcServer

/**
 * [ServerBuilder] decorator which adds the resulting [Server] instance to the environment's lifecycle.
 */
class DropwizardServerBuilder(
    private val environment: Environment,
    private val origin: ServerBuilder<*>,
    private val shutdownPeriod: Duration
) : ServerBuilder<DropwizardServerBuilder>() {

    override fun directExecutor(): DropwizardServerBuilder {
        origin.directExecutor()
        return this
    }

    override fun executor(executor: Executor?): DropwizardServerBuilder {
        origin.executor(executor)
        return this
    }

    override fun addService(service: ServerServiceDefinition): DropwizardServerBuilder {
        // TODO configure io.grpc.ServerInterceptor to collect dropwizard metrics
        // TODO configure io.grpc.ServerInterceptor to send rpc call and exception events to logback
        origin.addService(service)
        return this
    }

    override fun addService(bindableService: BindableService): DropwizardServerBuilder {
        // TODO configure io.grpc.ServerInterceptor to collect dropwizard metrics
        // TODO configure io.grpc.ServerInterceptor to send rpc call and exception events to logback
        origin.addService(bindableService)
        return this
    }

    override fun intercept(interceptor: ServerInterceptor): DropwizardServerBuilder {
        origin.intercept(interceptor)
        return this
    }

    override fun addTransportFilter(filter: ServerTransportFilter): DropwizardServerBuilder {
        origin.addTransportFilter(filter)
        return this
    }

    override fun addStreamTracerFactory(factory: Factory): DropwizardServerBuilder {
        origin.addStreamTracerFactory(factory)
        return this
    }

    override fun fallbackHandlerRegistry(fallbackRegistry: HandlerRegistry?): DropwizardServerBuilder {
        origin.fallbackHandlerRegistry(fallbackRegistry)
        return this
    }

    override fun useTransportSecurity(certChain: File, privateKey: File): DropwizardServerBuilder {
        origin.useTransportSecurity(certChain, privateKey)
        return this
    }

    override fun decompressorRegistry(registry: DecompressorRegistry?): DropwizardServerBuilder {
        origin.decompressorRegistry(registry)
        return this
    }

    override fun compressorRegistry(registry: CompressorRegistry?): DropwizardServerBuilder {
        origin.compressorRegistry(registry)
        return this
    }

    override fun build(): Server {
        val server: Server = origin.build()
        environment.lifecycle().manage(ManagedGrpcServer(server, shutdownPeriod))
        return server
    }
}