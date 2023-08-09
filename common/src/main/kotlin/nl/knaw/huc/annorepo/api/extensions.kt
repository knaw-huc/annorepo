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