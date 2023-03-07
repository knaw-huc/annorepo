package nl.knaw.huc.annorepo.api

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
    fun `there should be an editor role`() {
        assertThat(Role.EDITOR.roleName).isEqualTo("editor")
    }
}