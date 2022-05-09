package nl.knaw.huc.annorepo.db

import nl.knaw.huc.annorepo.api.AnnotationData
import org.jdbi.v3.json.EncodedJson
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper
import org.jdbi.v3.sqlobject.customizer.Bind
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys
import org.jdbi.v3.sqlobject.statement.SqlQuery
import org.jdbi.v3.sqlobject.statement.SqlUpdate

interface AnnotationDao {

    @SqlUpdate("insert into annotations (container_id,name,content,created,modified) values (:container_id,:name,:content,now(),now())")
    @GetGeneratedKeys("id")
    fun add(
        @Bind("container_id") containerId: Long,
        @Bind("name") name: String,
        @EncodedJson @Bind("content") content: String
    ): Long

    @SqlQuery("select name from annotations where id = :id")
    fun findNameById(@Bind("id") id: Long): String

    @SqlQuery("select id,name,content,created,modified from annotations where container_id = :container_id and name = :name limit 1")
    @RegisterBeanMapper(AnnotationData::class)
    fun findByContainerIdAndName(
        @Bind("container_id") containerId: Long,
        @Bind("name") annotationName: String
    ): AnnotationData?

    @SqlQuery("select id,name,content,created,modified from annotations where id = :id")
    @RegisterBeanMapper(AnnotationData::class)
    fun findById(@Bind("id") id: Long): AnnotationData

    @SqlQuery("select count(*)>0 from annotations where name = :name and container_id = :container_id")
    fun existsWithNameInContainer(@Bind("name") name: String, @Bind("container_id") containerId: Long): Boolean

    @SqlUpdate("delete from annotations where name = :name and container_id = :container_id")
    fun deleteByContainerIdAndName(@Bind("container_id") containerId: Long, @Bind("name") name: String)

    @SqlQuery("select c.name as containerName, a.name as annotationName from annotations a join containers c on a.container_id=c.id order by containerName,annotationName")
    @RegisterBeanMapper(AnnotationRecord::class)
    fun allAnnotations(): List<AnnotationRecord>

    @SqlQuery("select id from annotations where name = :name and container_id = :container_id")
    fun findIdByContainerIdAndName(@Bind("container_id") containerId: Long, @Bind("name") name: String): Long
}