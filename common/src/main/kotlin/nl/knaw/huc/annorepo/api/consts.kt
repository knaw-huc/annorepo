package nl.knaw.huc.annorepo.api

const val ANNO_JSONLD_URL = "http://www.w3.org/ns/anno.jsonld"
const val LDP_JSONLD_URL = "http://www.w3.org/ns/ldp.jsonld"

enum class Role(val roleName: String) {
    GUEST(roleName = "guest"),
    EDITOR(roleName = "editor"),
    ADMIN(roleName = "admin"),
    ROOT(roleName = "root")
}