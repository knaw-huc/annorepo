package nl.knaw.huc.annorepo.api

import org.junit.jupiter.api.Test
import org.assertj.core.api.Assertions.assertThat
import org.slf4j.LoggerFactory
import nl.knaw.huc.annorepo.protocol.GreeterOuterClass.HelloReply

internal class ProtoBufTest {
    private val log = LoggerFactory.getLogger(javaClass)

    @Test
    fun `test HelloReply`() {
        val helloReply = HelloReply.newBuilder().setMessage("Hello World").build()
        assertThat(helloReply.message).isEqualTo("Hello World")
    }
}
