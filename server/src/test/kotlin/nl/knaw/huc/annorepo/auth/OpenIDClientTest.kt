package nl.knaw.huc.annorepo.auth

import java.security.interfaces.RSAPublicKey
import kotlin.test.DefaultAsserter.fail
import org.junit.jupiter.api.Test
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import io.github.nefilim.kjwt.JWTKeyID
import io.github.nefilim.kjwt.jwks.JWKError
import io.github.nefilim.kjwt.jwks.WellKnownJWKSProvider
import io.github.nefilim.kjwt.jwks.WellKnownJWKSProvider.downloadJWKS
import io.github.nefilim.kjwt.jwks.WellKnownJWKSProvider.getJWKProvider
import io.github.nefilim.kjwt.jwks.getKey
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.apache.logging.log4j.kotlin.logger

class OpenIDClientTest {

    @Test
    fun `test openid with invalid token`() {
        val c = OpenIDClient(
            configurationURL = "https://authentication.clariah.nl/.well-known/openid-configuration"
        )
        val user = c.userForToken("thistokenisinvalid")
        logger.info { user }
        assert(user.isLeft)
    }

//    @Test
    fun `test openid with access token`() {
        val c = OpenIDClient(
            configurationURL = "https://connect.surfconext.nl/.well-known/openid-configuration"
        )
        val jwks = c.openIDConfig["jwks_uri"].toString()
        logger.info { jwks }

        val accessToken =
            "eyJhbGciOiJFUzI1NiIsImtpZCI6ImJFRTFVRGRIWkhJM01WUm9VVzlXVG14bVZVaDFPRzFTYVZrNU5XNUVSRGRXUkhoVFNESkNlR2gyUVEifQ.eyJzY29wZSI6ICJvcGVuaWQgcHJvZmlsZSBlbWFpbCBlZHVwZXJzb25fYXNzdXJhbmNlIGVkdXBlcnNvbl9lbnRpdGxlbWVudCBlZHVwZXJzb25fb3JjaWQgZWR1cGVyc29uX3ByaW5jaXBhbF9uYW1lIGVkdXBlcnNvbl9zY29wZWRfYWZmaWxpYXRpb24gdm9wZXJzb25fZXh0ZXJuYWxfYWZmaWxpYXRpb24gdm9wZXJzb25fZXh0ZXJuYWxfaWQgdm9wZXJzb25faWQgYWFyYyBzc2hfcHVibGljX2tleSBvcmNpZCB1aWQiLCAiYXVkIjogWyJBUFAtMkE0QTQxRjctM0EzQS00QzgxLUJFOEQtRjY1MERFOUI0NzdCIl0sICJqdGkiOiAiNzZhYmZlNDBjZGY4NGFiMzgyN2MyZThlNTdkMWFkNWIiLCAiY2xpZW50X2lkIjogIkFQUC0yQTRBNDFGNy0zQTNBLTRDODEtQkU4RC1GNjUwREU5QjQ3N0IiLCAic3ViIjogIjE4NjkyNjg3NDg4NTgxYzE3Y2IxOTdlYzY4NWJlZmE4ZDE4Zjg1N2VAc3JhbS5zdXJmLm5sIiwgIm5hbWUiOiAiSmFhcCBCbG9tIiwgImdpdmVuX25hbWUiOiAiSmFhcCIsICJmYW1pbHlfbmFtZSI6ICJCbG9tIiwgImVtYWlsIjogImpibG9tQGJlZWxkZW5nZWx1aWQubmwiLCAiZWR1cGVyc29uX2VudGl0bGVtZW50IjogWyJ1cm46bWFjZTpzdXJmLm5sOnNyYW06Z3JvdXA6a25hdyIsICJ1cm46bWFjZTpzdXJmLm5sOnNyYW06Z3JvdXA6a25hdzp0ZXN0X2Fubm9yZXBvOmFkbWluIiwgInVybjptYWNlOnN1cmYubmw6c3JhbTpncm91cDprbmF3OnRlc3RfYW5ub3JlcG8iLCAidXJuOm1hY2U6c3VyZi5ubDpzcmFtOmdyb3VwOmtuYXc6dGVzdF9hbm5vcmVwbzpzc2hvY19ubF9yb2xvZGV4LWdlbmVyYWwiLCAidXJuOm1hY2U6c3VyZi5ubDpzcmFtOmdyb3VwOnN1cmYtcmFtI3NyYW0uc3VyZi5ubCJdLCAiZWR1cGVyc29uX3ByaW5jaXBhbF9uYW1lIjogWyJqYmxvbTJAc3JhbS5zdXJmLm5sIl0sICJlZHVwZXJzb25fc2NvcGVkX2FmZmlsaWF0aW9uIjogWyJtZW1iZXJAc3JhbS5zdXJmLm5sIl0sICJ2b3BlcnNvbl9leHRlcm5hbF9pZCI6IFsiamJsb21AYmVlbGRlbmdlbHVpZC5ubCJdLCAidm9wZXJzb25faWQiOiBbIjE4NjkyNjg3NDg4NTgxYzE3Y2IxOTdlYzY4NWJlZmE4ZDE4Zjg1N2VAc3JhbS5zdXJmLm5sIl0sICJzc2hfcHVibGljX2tleSI6IFtdLCAidWlkIjogWyJqYmxvbTIiXSwgInNpZCI6ICJaMEZCUVVGQlFtOVpPRlJ0UkVJNFJUbDZOVFIzTkZWMlpuaFpNemxUYW1oVmJESjRhMkpGYVRrM1IxTkNOVUY2TUV0b1NHOUliR2N4TkdwYU9FMXlkMTlGZFd0aVNYUnBjQzFJWms5RlJsQTJiazF0TUUxQlkxTnBlRlpIYm5GRk9IWk5NV1JpTW5sRldXMTVabXhMTmtGcFRYVTNURVZvZFVWNWJqTk1hRjlxTFd4WE9EQjBaazl1YlhoS2RqbHpXbk15UnpaQ1Z6a3hjbTlqVGpGQ1VIbE5hRjltUzFSTWQxSnlTbDlCY1VaUU0zTnZiMTlYTms5TmNXRmpaRzQwT1U1Mk5HRllOVlUyTWpodlZtRlFNazF6TFhGUmVtUXhVRmxuVEc1c1VXTXdOVmxWY2xkUlNraHdlRFUwVEZkQllUZHRkREp4YjB0VlpqVXhRMkZUU0ZkWWNXcDVSUzE1TUc1WloyUXRha1pzYUhWZk1XaGFUV3N4U1hjelYwUnVUM2RNV2todGFsOUdTbTV4ZGxWcWFXaHFYMlZxYTFGMFNFMUVSVmw0UW5kVUxWQmtka040U0hSUVowUm9aM2x5Tkhab00xTm1hME5oUkcxdldGRllVbDluUFQwPSIsICJ0b2tlbl9jbGFzcyI6ICJhY2Nlc3NfdG9rZW4iLCAiaXNzIjogImh0dHBzOi8vcHJveHkuc3JhbS5zdXJmLm5sIiwgImlhdCI6IDE3NTEzNjg5MzQsICJleHAiOiAxNzUxMzcyNTM0fQ.Xeb1KgI2HXzziVId5wRu9ruGwu4WKZN0ai7N7A8V8NWnyRW63CcLoVjHz8ktkC-6zT4I0BErYLt59gYdymNBqw"
        val wellKnownContext = WellKnownJWKSProvider.WellKnownContext(jwks)
        runBlocking {
            either {
                val jwksDownloaded = downloadJWKS(wellKnownContext).bind()
                ensureNotNull(
                    Json.parseToJsonElement(jwksDownloaded).jsonObject["keys"]?.jsonArray?.first()?.jsonObject?.get(
                        "kid"
                    )
                ) { JWKError.NoSuchKey(JWTKeyID("missing kid?")) }
            }.fold({
                fail("failed to download JWKS json from Google: $it")
            }, {
                val kid = JWTKeyID(it.jsonPrimitive.content)
                logger.info { "downloading JWK for $kid from Google" }
                with(wellKnownContext.getJWKProvider<RSAPublicKey>(::downloadJWKS)) {
                    this.getKey(kid)
                }
            })

        }
    }

}
