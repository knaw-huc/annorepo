package nl.knaw.huc.annorepo.auth

import nl.knaw.huc.annorepo.api.Role

interface ContainerUserDAO {
    fun getUserRole(containerName: String, userName: String): Role?
}