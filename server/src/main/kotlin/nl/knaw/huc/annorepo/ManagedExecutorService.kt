package nl.knaw.huc.annorepo

import io.dropwizard.lifecycle.Managed
import org.slf4j.LoggerFactory
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit

class ManagedExecutorService(private val executorService: ExecutorService) : Managed {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun start() {
        log.debug("starting $executorService")
    }

    override fun stop() {
        log.debug("stopping $executorService")
        executorService.shutdown()
        try {
            if (!executorService.awaitTermination(800, TimeUnit.MILLISECONDS)) {
                executorService.shutdownNow()
            }
        } catch (e: InterruptedException) {
            executorService.shutdownNow()
        }
        log.debug("stopped")
    }

}
