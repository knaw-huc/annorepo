package nl.knaw.huc.annorepo.auth

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.security.Principal

data class User(private val name: String, val role: Role) : Principal {
    val log: Logger = LoggerFactory.getLogger(javaClass)
    override fun getName(): String = name
}