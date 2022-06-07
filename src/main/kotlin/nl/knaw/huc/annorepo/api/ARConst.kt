package nl.knaw.huc.annorepo.api

object ARConst {
    const val ES_INDEX_NAME = "annotations"

    enum class EnvironmentVariable {
        AR_SERVER_PORT,
        AR_EXTERNAL_BASE_URL,
        AR_DB_HOST,
        AR_DB_PORT,
        AR_DB_NAME,
        AR_DB_USER,
        AR_DB_PASSWD,
        AR_ES_HOST,
        AR_ES_PORT
    }

    const val ANNOTATION_MEDIA_TYPE = """application/ld+json;profile="http://www.w3.org/ns/anno.jsonld""""
}