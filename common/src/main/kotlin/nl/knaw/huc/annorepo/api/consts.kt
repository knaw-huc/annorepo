package nl.knaw.huc.annorepo.api

const val ANNO_JSONLD_URL = "http://www.w3.org/ns/anno.jsonld"
const val LDP_JSONLD_URL = "http://www.w3.org/ns/ldp.jsonld"

const val GRPC_METADATA_KEY_CONTAINER_NAME = "container-name"
const val GRPC_METADATA_KEY_API_KEY = "api-key"

enum class Role(val roleName: String) {
    GUEST(roleName = "guest"),
    EDITOR(roleName = "editor"),
    ADMIN(roleName = "admin"),
    ROOT(roleName = "root")
}