package nl.knaw.huc.annorepo.db

import org.jdbi.v3.sqlobject.config.RegisterBeanMapper
import org.jdbi.v3.sqlobject.customizer.Bind
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys
import org.jdbi.v3.sqlobject.statement.SqlQuery
import org.jdbi.v3.sqlobject.statement.SqlUpdate
import java.time.Instant
import java.util.*

interface AnnotationContainerDao {

    @SqlUpdate("create table annotation_containers (id serial primary key, name varchar(100), created_on timestamp)")
    fun createTable()

    @SqlUpdate("insert into annotation_containers (id, name) values (:id, :name)")
    fun insert(@Bind("id") id: Int, @Bind("name") name: String)

    class IdCreationTime {
        var id: Long = 0
        var time: Date = Date.from(Instant.now())

        override fun toString(): String = "id:$id, time:$time"
    }

    @SqlUpdate("insert into annotation_containers (name,created_on) values (:name,now())")
    @GetGeneratedKeys("id", "created_on")
    @RegisterBeanMapper(IdCreationTime::class)
    fun add(@Bind("name") name: String): IdCreationTime

    @SqlQuery("select name from annotation_containers where id = :id")
    fun findNameById(@Bind("id") id: Int): String

    @SqlQuery("select name from annotation_containers order by name")
    fun getNames(): List<String>
}