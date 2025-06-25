package nl.knaw.huc.annorepo.auth

import jakarta.ws.rs.client.ClientBuilder
import jakarta.ws.rs.client.WebTarget
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.ibm.asyncutil.util.Either
import org.glassfish.jersey.client.filter.EncodingFilter
import org.glassfish.jersey.message.GZipEncoder

class OpenIDClient(configurationURL: String) {
    val openIDConfig = getOpenIDConfiguration(configurationURL)

    fun userForToken(userToken: String?): Either<OpenIDTokenError, OpenIDUser> {
        val webTarget: WebTarget = ClientBuilder.newClient().apply {
            register(GZipEncoder::class.java)
            register(EncodingFilter::class.java)
        }.target(openIDConfig["userinfo_endpoint"].toString())
        val response = webTarget.request().header("Authorization", "Bearer $userToken").get()
//        println(response.headers.entries.joinToString("\n"))
//        println(response.readEntity(String::class.java))
        val entityAsJson = response.readEntity(String::class.java)
        val responseEntity: Map<String, Any> = oMapper.readValue(entityAsJson)
        when (response.status) {
            200 -> {
                val userName =
                    responseEntity["eppn"]?.toString()
                        ?: responseEntity["email"]?.toString()
                        ?: responseEntity["sub"]?.toString()
                        ?: ":no-username:"
                return Either.right(OpenIDUser(name = userName, userInfo = responseEntity))
            }

            401 -> return Either.left(OpenIDTokenError("The token was not recognized: responseBody=$responseEntity"))

            403 -> return Either.left(OpenIDTokenError("The token was not valid: responseBody=$responseEntity"))

            else -> return Either.left(OpenIDTokenError("Unexpected response status: ${response.status}, responseBody=$responseEntity"))
        }
    }

    data class OpenIDTokenError(val message: String)

    companion object {
        val oMapper = jacksonObjectMapper()

        fun getOpenIDConfiguration(openIdConfigurationURL: String): Map<String, Any> {
            val webTarget: WebTarget = ClientBuilder.newClient().apply {
                register(GZipEncoder::class.java)
                register(EncodingFilter::class.java)
            }.target(openIdConfigurationURL)
            val response = webTarget.request().get()
            val entityAsJson = response.readEntity(String::class.java)
            return oMapper.readValue(entityAsJson)
        }
    }
}
