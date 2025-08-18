package nl.knaw.huc.annorepo.api

private val restrictedChars =
    setOf(';', ',', '/', '?', ':', '@', '&', '=', '+', '$', '.', '!', '~', '*', '\'', '(', ')', '#', '"', '%')

fun String.isValidAnnotationName(): Boolean {
    val isAscii = this.all { it.code in 0..127 }
    val hasNoWhitespace = this.none { it.isWhitespace() }
    val hasNoRestrictedChars = this.none { it in restrictedChars }
    return isAscii && hasNoWhitespace && hasNoRestrictedChars
}

fun String.isValidContainerName(): Boolean = isValidAnnotationName()

@Suppress("UNCHECKED_CAST")
fun <T> Map<String, Any>.getNestedValue(key: String): T? {
    val keyParts = key.split(".")
    var value: Any? = this[keyParts.first()]

    for (k in keyParts.drop(1)) {
        value = (value as? Map<*, *>)?.get(k) ?: return null
    }
    return value as? T
}
