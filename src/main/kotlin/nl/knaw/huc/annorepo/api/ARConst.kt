package nl.knaw.huc.annorepo.api

object ARConst {
    const val ES_INDEX_NAME = "annotations"

    enum class EnvironmentVariable {
        AR_SERVER_PORT,
        AR_EXTERNAL_BASE_URL,
        AR_MONGODB_URL,
        AR_PAGE_SIZE
    }

    const val ANNOTATION_MEDIA_TYPE = """application/ld+json;profile="http://www.w3.org/ns/anno.jsonld""""
}