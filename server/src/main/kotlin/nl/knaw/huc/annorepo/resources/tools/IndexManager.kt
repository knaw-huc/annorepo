package nl.knaw.huc.annorepo.resources.tools

import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Indexes
import org.slf4j.LoggerFactory
import nl.knaw.huc.annorepo.api.ARConst
import nl.knaw.huc.annorepo.api.IndexChoreIndex
import nl.knaw.huc.annorepo.api.IndexType
import nl.knaw.huc.annorepo.api.IndexType.ASCENDING
import nl.knaw.huc.annorepo.api.IndexType.DESCENDING
import nl.knaw.huc.annorepo.api.IndexType.HASHED
import nl.knaw.huc.annorepo.api.IndexType.TEXT

class IndexManager(val mdb: MongoDatabase) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun startIndexCreation(
        containerName: String,
        fieldName: String,
        indexTypeName: String,
        indexType: IndexType,
        isJsonField: Boolean = true
    ): IndexChore {
        val container = mdb.getCollection(containerName)
        val fullFieldName = if (isJsonField) "${ARConst.ANNOTATION_FIELD}.${fieldName}" else fieldName
        val index = when (indexType) {
            HASHED -> Indexes.hashed(fullFieldName)
            ASCENDING -> Indexes.ascending(fullFieldName)
            DESCENDING -> Indexes.descending(fullFieldName)
            TEXT -> Indexes.text(fieldName)
            else -> throw RuntimeException("Cannot make an index with type $indexType")
        }
        return startIndexChore(
            IndexChore(
                id = choreId(containerName, fieldName, indexTypeName),
                container = container,
                fieldName = fullFieldName,
                index = index
            )
        )
    }

    fun getIndexChore(containerName: String, fieldName: String, indexTypeName: String): IndexChore? {
        val id = choreId(containerName, fieldName, indexTypeName)
        return IndexChoreIndex[id]
    }

    private fun choreId(containerName: String, fieldName: String, indexTypeName: String) =
        "$containerName/$fieldName/$indexTypeName".lowercase()

    private fun startIndexChore(chore: IndexChore): IndexChore {
        IndexChoreIndex[chore.id] = chore
        Thread(chore).start()
        return chore
    }

}