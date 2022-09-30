package nl.knaw.huc.annorepo.api

enum class IndexType(val mongoSuffix: String) {
    HASHED("hashed"),
    ASCENDING("1"),
    DESCENDING("-1"),
    TEXT("text");

    companion object {
        private val nameSet: Set<String> = IndexType.values()
            .map { it.name }
            .toSet()

        fun fromString(s: String): IndexType? {
            val name = s.uppercase()
            return if (nameSet.contains(name)) {
                IndexType.valueOf(name)
            } else {
                null
            }
        }
    }
}
