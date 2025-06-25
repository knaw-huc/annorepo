package nl.knaw.huc.annorepo.auth

import org.junit.jupiter.api.Test

class OpenIDClientTest {

    @Test
    fun `test openid with invalid token`() {
        val c = OpenIDClient(
            configurationURL = "https://authentication.clariah.nl/.well-known/openid-configuration"
        )
        val user = c.userForToken("thistokenisinvalid")
        println(user)
        assert(user.isLeft)
    }
}
