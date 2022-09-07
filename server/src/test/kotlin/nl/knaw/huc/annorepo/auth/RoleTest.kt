package nl.knaw.huc.annorepo.auth

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class RoleTest {
    @Test
    fun `there should be a root role`() {
        assertThat(Role.ROOT.roleName).isEqualTo("root")
    }

    @Test
    fun `there should be an admin role`() {
        assertThat(Role.ADMIN.roleName).isEqualTo("admin")
    }

    @Test
    fun `there should be a guest role`() {
        assertThat(Role.GUEST.roleName).isEqualTo("guest")
    }

    @Test
    fun `there should be a user role`() {
        assertThat(Role.USER.roleName).isEqualTo("user")
    }
}