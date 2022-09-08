package nl.knaw.huc.annorepo.auth

import java.security.Principal

abstract class User : Principal {
    internal abstract var name: String
    override fun getName(): String = name
}

data class BasicUser(override var name: String) : User()

data class RootUser(override var name: String = ":root:") : User()
