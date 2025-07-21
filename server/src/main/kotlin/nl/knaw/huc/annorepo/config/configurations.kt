package nl.knaw.huc.annorepo.config

import com.github.benmanes.caffeine.cache.CaffeineSpec

class AuthenticationConfiguration {
    var rootApiKey: String = "YouIntSeenMeRoit"
    var oidc: List<OIDCConfiguration> = listOf()
    var sram: SramConfiguration? = null
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
    var serverUrl: String = ""
    var requiredIssuer: String? = null
    var requiredAudience: String? = null

    fun toMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>()
        if (serverUrl.isNotEmpty()) {
            map.put("serverUrl", serverUrl)
        }
        requiredIssuer?.let { map.put("requiredIssuer", it) }
        requiredAudience?.let { map.put("requiredAudience", it) }
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
