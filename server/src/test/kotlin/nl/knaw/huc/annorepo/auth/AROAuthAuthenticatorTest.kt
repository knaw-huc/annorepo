package nl.knaw.huc.annorepo.auth

import org.junit.jupiter.api.Test
import nl.knaw.huc.annorepo.api.UserAddResults
import nl.knaw.huc.annorepo.api.UserEntry
import nl.knaw.huc.annorepo.dao.UserDAO

class AROAuthAuthenticatorTest {
    @Test
    fun `test authenticate`() {
        val a = AROAuthAuthenticator(userDAO = TestUserDAO())
        val optionalUser = a.authenticate("some api key")
        assert(optionalUser.isEmpty)
    }

    class TestUserDAO : UserDAO {
        override fun userForApiKey(apiKey: String?): User? = null

        override fun addUserEntries(userEntries: List<UserEntry>): UserAddResults {
            TODO("Not yet implemented")
        }

        override fun allUserEntries(): List<UserEntry> {
            TODO("Not yet implemented")
        }

        override fun deleteUsersByName(userNames: Collection<String>): Boolean {
            TODO("Not yet implemented")
        }

        override fun allGroupNames(): List<String> {
            TODO("Not yet implemented")
        }

    }

}