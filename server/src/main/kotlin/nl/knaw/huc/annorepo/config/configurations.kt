package nl.knaw.huc.annorepo.config

import jakarta.validation.Valid
import com.fasterxml.jackson.annotation.JsonProperty
import com.github.benmanes.caffeine.cache.CaffeineSpec
import org.jetbrains.annotations.NotNull

class AuthenticationConfiguration {
    var rootApiKey: String = "YouIntSeenMeRoit"
    var oidc: List<OIDCConfiguration> = listOf()
    var sram: SramConfiguration? = null

    @Valid
    @NotNull
    @JsonProperty
    var cachePolicy: CaffeineSpec? = null

    fun toMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>("internal" to "api-keys")
        if (oidc.isNotEmpty()) {
            map.put("oidc", oidc.map { it.toMap() })
        }
        sram?.let { map.put("sram", it.toMap()) }
        return map.toMap()
    }
}

class OIDCConfiguration {
    @Valid
    @NotNull
    @JsonProperty
    var name: String = ""

    @Valid
    @NotNull
    @JsonProperty
    var serverUrl: String = ""

    var requiredIssuer: String? = null
    var requiredAudiences: List<String>? = null

    fun toMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>("name" to name)
        if (serverUrl.isNotEmpty()) {
            map.put("serverUrl", serverUrl)
        }
        requiredIssuer?.let { map.put("requiredIssuer", it) }
        requiredAudiences?.let { map.put("requiredAudiences", "✅") }
        return map.toMap()
    }
}

class SramConfiguration {
    var introspectUrl: String = ""
    var applicationToken: String = ""

    fun toMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>()
        if (introspectUrl.isNotEmpty()) {
            map.put("introspectUrl", introspectUrl)
        }
        if (applicationToken.isNotEmpty()) {
            map.put("applicationToken", "✅")
        }
        return map.toMap()
    }
}
