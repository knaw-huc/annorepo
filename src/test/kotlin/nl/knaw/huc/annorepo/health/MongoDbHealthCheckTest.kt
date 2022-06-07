package nl.knaw.huc.annorepo.health

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.litote.kmongo.KMongo

internal class MongoDbHealthCheckTest {

    @Disabled
    @Test
    fun checkPassingHealthCheck() {
        val client = KMongo.createClient("mongodb://localhost/")
        val hc = MongoDbHealthCheck(client)
        val result = hc.check()
        assertThat(result).isNotNull
        assertThat(result!!.isHealthy).isTrue
    }

    @Disabled
    @Test
    fun checkFailingHealthCheck() {
        val client = KMongo.createClient("mongodb://idontexist/")
        val hc = MongoDbHealthCheck(client)
        val result = hc.check()
        assertThat(result).isNotNull
        assertThat(result!!.isHealthy).isFalse
    }
}