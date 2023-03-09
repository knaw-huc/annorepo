package nl.knaw.huc.annorepo.auth

import nl.knaw.huc.annorepo.api.ContainerUserEntry
import nl.knaw.huc.annorepo.api.Role

interface ContainerUserDAO {
    fun addContainerUser(containerName: String, userName: String, role: Role)
    fun getUserRole(containerName: String, userName: String): Role?
    fun getUsersForContainer(containerName: String): List<ContainerUserEntry>
    fun removeContainerUser(containerName: String, userName: String)
}