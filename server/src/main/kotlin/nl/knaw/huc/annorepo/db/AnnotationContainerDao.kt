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

    @SqlQuery("select id,name,created,modified from containers where name = :name limit 1")
    @RegisterBeanMapper(ContainerData::class)
    fun findByName(@Bind("name") name: String): ContainerData?

    @SqlQuery("select id,name,created,modified from containers where id = :id")
    @RegisterBeanMapper(ContainerData::class)
    fun findById(@Bind("id") id: Long): ContainerData

    @SqlQuery("select count(*)>0 from containers where name = :name")
    fun existsWithName(@Bind("name") name: String): Boolean

    @SqlUpdate("delete from containers where name = :name")
    fun deleteByName(@Bind("name") name: String)

    @SqlQuery("select count(*)=0 from annotations where container_id=(select id from containers where name = :name limit 1)")
    fun isEmpty(@Bind("name") name: String): Boolean

    @SqlQuery("select id from containers where name = :name limit 1")
    fun findIdByName(@Bind("name") containerName: String): Long?

}