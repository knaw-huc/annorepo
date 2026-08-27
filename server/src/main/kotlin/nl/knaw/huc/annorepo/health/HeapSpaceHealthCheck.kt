package nl.knaw.huc.annorepo.health

import java.lang.management.ManagementFactory
import java.lang.management.MemoryMXBean
import java.lang.management.MemoryUsage
import com.codahale.metrics.health.HealthCheck

class HeapSpaceHealthCheck(
    private val threshold: Double = 0.9 // 90%
) : HealthCheck() {

    private val memoryBean: MemoryMXBean = ManagementFactory.getMemoryMXBean()

    override fun check(): Result {
        val heapUsage: MemoryUsage = memoryBean.heapMemoryUsage

        val used = heapUsage.used.toDouble()
        val max = heapUsage.max.toDouble()

        if (max <= 0) {
            return Result.unhealthy("Max heap size is undefined")
        }

        val usageRatio = used / max

        return when {
            usageRatio >= threshold ->
                Result.unhealthy(
                    "Heap usage too high: %.2f%% (used=%d, max=%d)"
                        .format(usageRatio * 100, heapUsage.used, heapUsage.max)
                )

            else ->
                Result.healthy(
                    "Heap usage OK: %.2f%% (used=%d, max=%d)"
                        .format(usageRatio * 100, heapUsage.used, heapUsage.max)
                )
        }
    }
}