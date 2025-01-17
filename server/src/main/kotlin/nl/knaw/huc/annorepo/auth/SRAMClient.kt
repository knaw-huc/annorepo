package nl.knaw.huc.annorepo.auth

import jakarta.ws.rs.client.ClientBuilder
import jakarta.ws.rs.client.Entity
import jakarta.ws.rs.client.WebTarget
import jakarta.ws.rs.core.MultivaluedHashMap
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.ibm.asyncutil.util.Either
import org.glassfish.jersey.client.filter.EncodingFilter
import org.glassfish.jersey.message.GZipEncoder

class SRAMClient(private val applicationToken: String, private val sramIntrospectUrl: String) {

    fun userForToken(userToken: String?): Either<SramTokenError, SramUser> {
        val webTarget: WebTarget = ClientBuilder.newClient().apply {
            register(GZipEncoder::class.java)
            register(EncodingFilter::class.java)
        }.target(sramIntrospectUrl)

        val formEntity = Entity.form(MultivaluedHashMap<String, String>().apply { add("token", userToken) })
        val response = webTarget.request().header("Authorization", "Bearer $applicationToken")
            .post(formEntity)
        when (response.status) {
            200 -> {
                val entityAsJson = response.readEntity(String::class.java)
                val responseEntity: Map<String, Any> = oMapper.readValue(entityAsJson)
                when (val status = responseEntity["status"]?.toString()) {
                    "token-valid" -> {
                        val userName = responseEntity["username"]?.toString() ?: ":no-username:"
                        return Either.right(SramUser(name = userName, record = responseEntity))
                    }

                    in INVALID_TOKEN_STATUS_MESSAGES -> return Either.left(SramTokenError("SRAM says: ${INVALID_TOKEN_STATUS_MESSAGES[status]}"))

                    else -> return Either.left(SramTokenError("unexpected status on token verification: $status"))
                }
            }

            else -> return Either.left(SramTokenError("Unexpected response status: ${response.status}"))
        }
    }

    data class SramTokenError(val message: String)

    companion object {
        val oMapper = jacksonObjectMapper()
        val INVALID_TOKEN_STATUS_MESSAGES = mapOf(
            "token-unknown" to "Token is unknown or otherwise invalid",
            "token-expired" to "Token is expired",
            "user-suspended" to "Token is valid, but user is suspended due to inactivity",
            "token-not-connected" to "Application is not connected to any of the user's collaboration"
        )
    }
}
