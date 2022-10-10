package nl.knaw.huc.annorepo.api

object ARConst {

    const val APP_NAME = "AnnoRepo"

    enum class EnvironmentVariable {
        AR_SERVER_PORT,
        AR_EXTERNAL_BASE_URL,
        AR_MONGODB_URL,
        AR_DB_NAME,
        AR_PAGE_SIZE,
        AR_ROOT_API_KEY,
        AR_WITH_AUTHENTICATION,
        AR_PRETTY_PRINT,
    }

    const val ANNO_JSONLD_URL = "http://www.w3.org/ns/anno.jsonld"
    const val LDP_JSONLD_URL = "http://www.w3.org/ns/ldp.jsonld"
    const val ANNOTATION_MEDIA_TYPE = """application/ld+json;profile="$ANNO_JSONLD_URL""""
    const val CONTAINER_METADATA_COLLECTION = "_containerMetadata"
    const val USER_COLLECTION = "_users"
    const val SECURITY_SCHEME_NAME = "BearerAuth"

    const val ANNOTATION_FIELD = "annotation"
    const val ANNOTATION_NAME_FIELD = "annotation_name"
    const val CONTAINER_NAME_FIELD = "name"

}