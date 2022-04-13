package nl.knaw.huc.annorepo.health

import com.codahale.metrics.health.HealthCheck

class ServerHealthCheck : HealthCheck() {

    override fun check(): Result? =
        Result.healthy()

}
