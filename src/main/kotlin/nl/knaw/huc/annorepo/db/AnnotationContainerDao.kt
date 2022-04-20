package nl.knaw.huc.annorepo.db

import nl.knaw.huc.annorepo.api.ContainerData
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper
import org.jdbi.v3.sqlobject.customizer.Bind
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys
import org.jdbi.v3.sqlobject.statement.SqlQuery
import org.jdbi.v3.sqlobject.statement.SqlUpdate

interface AnnotationContainerDao {

    @SqlUpdate("insert into containers (name,created,modified) values (:name,now(),now())")
    @GetGeneratedKeys("id")
    fun add(@Bind("name") name: String): Long

    @SqlQuery("select name from containers where id = :id")
    fun findNameById(@Bind("id") id: Int): String

    @SqlQuery("select name from containers order by name")
    fun getNames(): List<String>

    @SqlQuery("select id,name from containers where name = :name limit 1")
    @RegisterBeanMapper(ContainerData::class)
    fun findByName(@Bind("name") name: String): ContainerData

    @SqlUpdate("delete from containers where name = :name")
    fun deleteByName(@Bind("name") name: String)

}