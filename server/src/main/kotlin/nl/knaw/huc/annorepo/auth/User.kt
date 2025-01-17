package nl.knaw.huc.annorepo.auth

import java.security.Principal
import nl.knaw.huc.annorepo.api.getNestedValue

abstract class User : Principal {
    internal abstract var name: String
    override fun getName(): String = name
}

data class BasicUser(override var name: String) : User()
data class SramUser(override var name: String, val record: Map<String, Any>) : User()

data class SramUser(override var name: String, val record: Map<String, Any>) : User() {
    val sramGroups: List<String> = groups(record)

    private fun groups(record: Map<String, Any>): List<String> {
        return record.getNestedValue<List<String>>("user.eduperson_entitlement") ?: emptyList()
    }
}

data class OpenIDUser(override var name: String, val userInfo: Map<String, Any>) : User() {

}

data class RootUser(override var name: String = ":root:") : User()
