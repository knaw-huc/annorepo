package nl.knaw.huc.annorepo.health

import com.codahale.metrics.health.HealthCheck
import com.mongodb.ReadPreference
import com.mongodb.kotlin.client.MongoClient

class MongoDbHealthCheck(private val mongoClient: MongoClient) : HealthCheck() {

    public override fun check(): Result? =
        try {
            mongoClient.listDatabaseNames().toList()
            val hasReadableServer = mongoClient.clusterDescription.hasReadableServer(ReadPreference.nearest())
            val hasWritableServer = mongoClient.clusterDescription.hasWritableServer()
            if (hasReadableServer && hasWritableServer) {
                Result.healthy()
            } else {
                val message = "mongodbserver not read/writable"
                Result.unhealthy(message)
            }
        } catch (e: Exception) {
            Result.unhealthy(e.message)
        }

}
