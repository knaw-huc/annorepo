import nl.knaw.huc.annorepo.health.MongoDbHealthCheck
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.litote.kmongo.KMongo

internal class MongoDbHealthCheckTest {

    @Test
    fun checkPassingHealthCheck() {
        val client = KMongo.createClient("mongodb://localhost/")
        val hc = MongoDbHealthCheck(client)
        val result = hc.check()
        assertThat(result).isNotNull
        assertThat(result!!.isHealthy).isTrue
    }

    @Test
    fun checkFailingHealthCheck() {
        val client = KMongo.createClient("mongodb://idontexist/")
        val hc = MongoDbHealthCheck(client)
        val result = hc.check()
        assertThat(result).isNotNull
        assertThat(result!!.isHealthy).isFalse
    }
}