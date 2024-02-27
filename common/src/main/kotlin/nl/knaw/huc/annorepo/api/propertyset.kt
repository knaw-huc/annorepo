package nl.knaw.huc.annorepo.api

typealias PropertySet = Map<String, Any?>

inline fun <reified T : Any> PropertySet.required(key: String): T {
    val value: Any = get(key) ?: error("Cannot find key <$key>")
    return value as? T ?: error("Value for key <$key> is not a ${T::class}")
}

inline fun <reified T : Any> PropertySet.required(key0: String, vararg otherKeys: String): T =
    required(listOf(key0) + otherKeys.toList())

inline fun <reified T : Any> PropertySet.required(keys: List<String>): T =
    when {
        keys.isEmpty() -> error("no keys supplied")
        else -> keys.dropLast(1)
            .fold(this, PropertySet::required)
            .required(keys.last())
    }

inline fun <reified T : Any> PropertySet.optional(key: String): T? {
    val value: Any? = get(key)
    return if (value == null) {
        null
    } else {
        value as? T ?: error("Value for key <$key> is not a ${T::class}")
    }
}

inline fun <reified T : Any> PropertySet.optional(key0: String, vararg otherKeys: String): T? =
    optional(listOf(key0) + otherKeys.toList())

inline fun <reified T : Any> PropertySet.optional(keys: List<String>): T? =
    when {
        keys.isEmpty() -> error("no keys supplied")
        else -> {
            var goOn = true
            var keyIdx = 0
            var propertySet: PropertySet = this
            while (goOn) {
                val value = propertySet[keys[keyIdx++]]
                if (value != null) {
                    propertySet = value as PropertySet
                }
                goOn = value != null && keyIdx < keys.size - 1
            }
            propertySet.optional<T>(keys.last())
        }
    }
