package nl.knaw.huc.annorepo.db

import org.jdbi.v3.core.kotlin.KotlinPlugin
import org.jdbi.v3.sqlobject.SqlObjectPlugin
import org.jdbi.v3.sqlobject.kotlin.KotlinSqlObjectPlugin
import org.jdbi.v3.testing.junit5.JdbiExtension
import org.junit.jupiter.api.extension.RegisterExtension
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class AnnotationContainerDaoTest {
    @RegisterExtension
    val h2Extension: JdbiExtension = JdbiExtension.h2()
        .withPlugin(SqlObjectPlugin())
        .withPlugin(KotlinPlugin())
        .withPlugin(KotlinSqlObjectPlugin())

    private val log: Logger = LoggerFactory.getLogger(javaClass)

//    @Test
//    fun testDao() {
//        val jdbi = h2Extension.jdbi
//        val dao = jdbi.onDemand<AnnotationContainerDao>()
//        dao.createTable()
//        val name1 = "The secret to life, the universe and everything"
//        dao.insert(42, name1)
//        val name2 = "Will Riker"
//        dao.insert(2, name2)
//        val name = dao.findNameById(42)
//        assertThat(name).isEqualTo(name1)
//        assertThat(dao.getNames()).contains(name1, name2)
//        val generatedId = dao.add("container3")
//        log.info("generatedId={}", generatedId)
//    }
//
//    @Test
//    fun testDao2() {
//        val jdbi = h2Extension.jdbi
//        val dao = jdbi.onDemand<AnnotationContainerDao>()
//        dao.createTable()
//        val generated = dao.add("container")
//        log.info("generated={}", generated)
//        val generated2 = dao.add("container2")
//        log.info("generated={}", generated2)
//    }

}