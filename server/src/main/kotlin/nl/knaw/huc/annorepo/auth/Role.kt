package nl.knaw.huc.annorepo.auth

enum class Role(val roleName: String) {
    GUEST(roleName = "guest"),
    USER(roleName = "user"),
    ADMIN(roleName = "admin"),
    ROOT(roleName = "root")
}