package nl.knaw.huc.annorepo.auth

import java.net.URL
import java.security.Key
import jakarta.ws.rs.client.ClientBuilder
import jakarta.ws.rs.client.WebTarget
import arrow.core.Either
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.nimbusds.jose.jwk.AsymmetricJWK
import com.nimbusds.jose.jwk.JWKSet
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jws
import io.jsonwebtoken.JwsHeader
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.JwtParser
import io.jsonwebtoken.JwtParserBuilder
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.LocatorAdapter
import org.glassfish.jersey.client.filter.EncodingFilter
import org.glassfish.jersey.message.GZipEncoder
import nl.knaw.huc.annorepo.api.optional

class OpenIDClient(
    val name: String,
    configurationURL: String,
    val requiredIssuer: String? = null,
    val requiredAudiences: List<String>? = null
) {
    val openIDConfig = getOpenIDConfiguration(configurationURL)

    fun userForToken(token: String?): Either<OpenIDTokenError, OpenIDUser> =
        if (token.isJWT()) userForJWT(token) else userForAccessToken(token)

    private fun userForAccessToken(token: String?): Either<OpenIDTokenError, OpenIDUser> {
        val webTarget: WebTarget = ClientBuilder.newClient().apply {
            register(GZipEncoder::class.java)
            register(EncodingFilter::class.java)
        }.target(openIDConfig["userinfo_endpoint"].toString())
        val response = webTarget.request().header("Authorization", "Bearer $token").get()
        val entityAsJson = response.readEntity(String::class.java)
        val responseEntity: Map<String, Any> = oMapper.readValue(entityAsJson)
        when (response.status) {
            200 -> {
                val userName =
                    responseEntity["email"]?.toString()
                        ?: responseEntity["eppn"]?.toString()
                        ?: responseEntity["edupersontargetedid"]?.toString()
                        ?: responseEntity["sub"]?.toString()
                        ?: ":no-username:"
                return Either.Right(OpenIDUser(name = userName, userInfo = responseEntity))
            }

            401 -> return Either.Left(OpenIDTokenError("The token was not recognized: responseBody=$responseEntity"))

            403 -> return Either.Left(OpenIDTokenError("The token was not valid: responseBody=$responseEntity"))

            else -> return Either.Left(OpenIDTokenError("Unexpected response status: ${response.status}, responseBody=$responseEntity"))
        }
    }

    private fun userForJWT(jwt: String?): Either<OpenIDTokenError, OpenIDUser> {
        val jwks = openIDConfig["jwks_uri"].toString()
        val keyLocator = MyKeyLocator(URL(jwks))

        val claimsBuilders =
            requiredAudiences
                ?.map { audience -> jwtParserBuilder(keyLocator).requireAudience(audience) }
                ?: listOf(jwtParserBuilder(keyLocator))

        val claimsEithers = claimsBuilders
            .map { it.build().functionalParseSignedClaims(jwt) }
        val rightClaimsEither = claimsEithers.firstOrNull { it.isRight() }
        return if (rightClaimsEither == null) {
            Either.Left(OpenIDTokenError(claimsEithers.first().leftOrNull()?.message ?: ""))
        } else {
            val claims = rightClaimsEither.getOrNull()?.payload!!
            val name = claims.optional<String>("email")
                ?: claims.optional<List<String>>("voperson_external_id")?.first()
                ?: claims.optional<String>("eppn")
                ?: claims.optional<String>("sub")
                ?: ":no-username:"
            Either.Right(OpenIDUser(name = name, userInfo = claims))
        }

    }

    private fun jwtParserBuilder(keyLocator: MyKeyLocator): JwtParserBuilder = Jwts.parser()
        .keyLocator(keyLocator)
        .requireIssuer(requiredIssuer)

    data class OpenIDTokenError(val message: String)

    companion object {
        val oMapper = jacksonObjectMapper()

        // just a quick check, obviously there's more to a jwt
        private fun String?.isJWT(): Boolean = this?.count { it == '.' } == 2

        class MyKeyLocator(val jwksUrl: URL) : LocatorAdapter<Key>() {
            public override fun locate(jwsHeader: JwsHeader): Key? =
                JWKSet.load(jwksUrl)
                    .getKeyByKeyId(jwsHeader.keyId)
                    ?.let { (it as AsymmetricJWK).toPublicKey() }
        }

        fun getOpenIDConfiguration(openIdConfigurationURL: String): Map<String, Any> {
            val webTarget: WebTarget = ClientBuilder.newClient().apply {
                register(GZipEncoder::class.java)
                register(EncodingFilter::class.java)
            }.target(openIdConfigurationURL)
            val response = webTarget.request().get()
            val entityAsJson = response.readEntity(String::class.java)
            return oMapper.readValue(entityAsJson)
        }

        private fun JwtParser.functionalParseSignedClaims(jwt: String?): Either<OpenIDTokenError, Jws<Claims>> =
            try {
                Either.Right(parseSignedClaims(jwt))
            } catch (e: JwtException) {
                Either.Left(OpenIDTokenError(e.message ?: e.javaClass.toString()))
            }
    }
}

