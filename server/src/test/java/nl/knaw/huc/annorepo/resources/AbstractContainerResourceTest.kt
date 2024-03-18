package nl.knaw.huc.annorepo.resources

import java.security.Principal
import jakarta.ws.rs.NotAuthorizedException
import jakarta.ws.rs.core.SecurityContext
import org.junit.jupiter.api.Test
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import nl.knaw.huc.annorepo.api.ContainerMetadata
import nl.knaw.huc.annorepo.config.AnnoRepoConfiguration
import nl.knaw.huc.annorepo.dao.ContainerDAO
import nl.knaw.huc.annorepo.dao.ContainerUserDAO
import nl.knaw.huc.annorepo.resources.tools.ContainerAccessChecker

class AbstractContainerResourceTest {
    private val containerName = "an-existing-container"

    class TestResource(
        configuration: AnnoRepoConfiguration,
        containerDAO: ContainerDAO,
        containerAccessChecker: ContainerAccessChecker
    ) : AbstractContainerResource(configuration, containerDAO, containerAccessChecker) {
        fun isReadable(containerName: String, principal: Principal): Boolean {
            val securityContext = mockk<SecurityContext>()
            every { securityContext.userPrincipal } returns principal
            securityContext.checkUserHasReadRightsInThisContainer(containerName)
            return true
        }
    }

    @Test
    fun `A user with a valid api key should have read access to a container that is read-only for anyone`() {
        val userName = "a-user"

        val mockPrincipal = mockk<Principal>()
        every { mockPrincipal.name } returns userName

        val mockContainerUserDAO = mockk<ContainerUserDAO>()
        every {
            mockContainerUserDAO.getUserRole(
                containerName,
                userName
            )
        } returns null // user has no role in this container

        val containerAccessChecker = ContainerAccessChecker(mockContainerUserDAO)

        val mockContainerMetadata = mockk<ContainerMetadata>()
        every { mockContainerMetadata.isReadOnlyForAnonymous } returns true

        val mockContainerDAO = mockk<ContainerDAO>()
        every { mockContainerDAO.containerExists(containerName) } returns true
        every { mockContainerDAO.getContainerMetadata(containerName) } returns mockContainerMetadata

        val mockConfiguration = mockk<AnnoRepoConfiguration>()

        val resource = TestResource(mockConfiguration, mockContainerDAO, containerAccessChecker)
        val hasReadAccess: Boolean = resource.isReadable(containerName, mockPrincipal)

        assertThat(hasReadAccess).isTrue()
    }

    @Test
    fun `A user with a valid api key should not have read access to a container that is not read-only for anyone`() {
        val userName = "a-user"

        val mockPrincipal = mockk<Principal>()
        every { mockPrincipal.name } returns userName

        val mockContainerUserDAO = mockk<ContainerUserDAO>()
        every { mockContainerUserDAO.getUserRole(containerName, userName) } returns null

        val containerAccessChecker = ContainerAccessChecker(mockContainerUserDAO)

        val mockContainerMetadata = mockk<ContainerMetadata>()
        every { mockContainerMetadata.isReadOnlyForAnonymous } returns false

        val mockContainerDAO = mockk<ContainerDAO>()
        every { mockContainerDAO.containerExists(containerName) } returns true
        every { mockContainerDAO.getContainerMetadata(containerName) } returns mockContainerMetadata

        val mockConfiguration = mockk<AnnoRepoConfiguration>()
        every { mockConfiguration.withAuthentication } returns true

        val resource = TestResource(mockConfiguration, mockContainerDAO, containerAccessChecker)
        assertThatExceptionOfType(NotAuthorizedException::class.java).isThrownBy {
            resource.isReadable(containerName, mockPrincipal)
        }.withMessage("HTTP 401 Unauthorized")
    }

}