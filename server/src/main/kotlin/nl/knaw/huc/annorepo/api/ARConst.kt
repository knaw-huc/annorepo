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
        AR_RANGE_SELECTOR_TYPE,
        AR_GRPC_HOSTNAME,
        AR_GRPC_PORT,
        AR_APPLICATION_TOKEN
    }

    const val ANNOTATION_MEDIA_TYPE = """application/ld+json;profile="$ANNO_JSONLD_URL""""
    const val CONTAINER_METADATA_COLLECTION = "_containerMetadata"
    const val USER_COLLECTION = "_users"
    const val CONTAINER_USER_COLLECTION = "_container_users"
    const val CUSTOM_QUERY_COLLECTION = "_custom_queries"
    const val SECURITY_SCHEME_NAME = "BearerAuth"

    const val ANNOTATION_FIELD = "annotation"
    const val ANNOTATION_NAME_FIELD = "annotation_name"
    const val CONTAINER_NAME_FIELD = "name"
}