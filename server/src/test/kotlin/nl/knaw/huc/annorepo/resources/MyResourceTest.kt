package nl.knaw.huc.annorepo.resources

import java.util.SortedMap
import java.util.TreeMap
import jakarta.ws.rs.core.SecurityContext
import org.junit.jupiter.api.Test
import com.mongodb.client.MongoCollection
import com.mongodb.client.result.UpdateResult
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.bson.Document
import nl.knaw.huc.annorepo.api.AnnotationIdentifier
import nl.knaw.huc.annorepo.api.ContainerMetadata
import nl.knaw.huc.annorepo.api.ContainerUserEntry
import nl.knaw.huc.annorepo.api.IndexConfig
import nl.knaw.huc.annorepo.api.Role
import nl.knaw.huc.annorepo.api.UserAccessEntry
import nl.knaw.huc.annorepo.api.WebAnnotationAsMap
import nl.knaw.huc.annorepo.dao.ContainerDAO
import nl.knaw.huc.annorepo.dao.ContainerUserDAO

class MyResourceTest {

    private val context = mockk<SecurityContext>()
    private val resource = MyResource(TestContainerDAO(), TestContainerUserDAO())

    @Test
    fun `root user has root access to all containers`() {
        every { context.userPrincipal.name } returns "root"
        val result = resource.getAccessibleContainers(context)
        val expected = TreeMap<String, List<String>>().apply {
            put("ROOT", allContainerNames)
        }
        assertThat(result.entity).isEqualTo(expected)
    }

    @Test
    fun `anonymous user has guest access to public container`() {
        every { context.userPrincipal } returns null
        val result = resource.getAccessibleContainers(context)
        val expected = TreeMap<String, List<String>>().apply {
            put("GUEST", listOf(PUBLIC_CONTAINER_NAME))
        }
        assertThat(result.entity).isEqualTo(expected)
    }

    @Test
    fun `non-root user has admin access to by-invitation-only container, guest access to public container`() {
        every { context.userPrincipal.name } returns "user"
        val result = resource.getAccessibleContainers(context)
        val expected = TreeMap<String, List<String>>().apply {
            put("ADMIN", listOf(BY_INVITATION_CONTAINER_NAME))
            put("GUEST", listOf(PUBLIC_CONTAINER_NAME))
        }
        assertThat(result.entity).isEqualTo(expected)
    }

    companion object {
        const val SECRET_CONTAINER_NAME = "secret"
        const val PUBLIC_CONTAINER_NAME = "public"
        const val BY_INVITATION_CONTAINER_NAME = "by-invitation-only"
        private val allContainerNames =
            listOf(SECRET_CONTAINER_NAME, PUBLIC_CONTAINER_NAME, BY_INVITATION_CONTAINER_NAME)
    }

    class TestContainerDAO : ContainerDAO {
        private val secretMetadata = mockk<ContainerMetadata>()
        private val publicMetadata = mockk<ContainerMetadata>()
        private val byInvitationMetadata = mockk<ContainerMetadata>()

        init {
            every { secretMetadata.isReadOnlyForAnonymous } returns false
            every { publicMetadata.isReadOnlyForAnonymous } returns true
            every { byInvitationMetadata.isReadOnlyForAnonymous } returns false
        }

        override fun listCollectionNames(): List<String> = allContainerNames

        override fun getContainerMetadata(containerName: String): ContainerMetadata =
            when (containerName) {
                SECRET_CONTAINER_NAME -> secretMetadata
                PUBLIC_CONTAINER_NAME -> publicMetadata
                else -> byInvitationMetadata
            }

        override fun updateContainerMetadata(
            containerName: String,
            containerMetadata: ContainerMetadata
        ): UpdateResult {
            TODO("Not yet implemented")
        }

        override fun containerExists(containerName: String): Boolean {
            TODO("Not yet implemented")
        }

        override fun getCollectionStats(containerName: String): Document {
            TODO("Not yet implemented")
        }

        override fun getAnnotationFields(containerName: String): SortedMap<String, Int> {
            TODO("Not yet implemented")
        }

        override fun createCollection(containerName: String) {
            TODO("Not yet implemented")
        }

        override fun getContainerMetadataCollection(): MongoCollection<ContainerMetadata> {
            TODO("Not yet implemented")
        }

        override fun getDistinctValues(containerName: String, field: String): List<Any> {
            TODO("Not yet implemented")
        }

        override fun getCollection(containerName: String): MongoCollection<Document> {
            TODO("Not yet implemented")
        }

        override fun addAnnotationsInBatch(
            containerName: String,
            annotations: List<WebAnnotationAsMap>
        ): List<AnnotationIdentifier> {
            TODO("Not yet implemented")
        }

        override fun dropContainerIndex(containerName: String, indexId: String) {
            TODO("Not yet implemented")
        }

        override fun getContainerIndexDefinition(containerName: String, indexId: String): Any {
            TODO("Not yet implemented")
        }

        override fun indexConfig(containerName: String, mongoIndexName: String, indexId: String): IndexConfig {
            TODO("Not yet implemented")
        }

    }

    class TestContainerUserDAO : ContainerUserDAO {
        override fun getUserRoles(userName: String): List<UserAccessEntry> =
            when (userName) {
                "root" -> allContainerNames.map {
                    UserAccessEntry("root", it, Role.ROOT)
                }

                else -> listOf(UserAccessEntry(userName, BY_INVITATION_CONTAINER_NAME, Role.ADMIN))
            }

        override fun addContainerUser(containerName: String, userName: String, role: Role) {
            TODO("Not yet implemented")
        }

        override fun getUserRole(containerName: String, userName: String): Role? {
            TODO("Not yet implemented")
        }

        override fun getUsersForContainer(containerName: String): List<ContainerUserEntry> {
            TODO("Not yet implemented")
        }

        override fun removeContainerUser(containerName: String, userName: String) {
            TODO("Not yet implemented")
        }

        override fun getAll(): List<UserAccessEntry> {
            TODO("Not yet implemented")
        }
    }

}

