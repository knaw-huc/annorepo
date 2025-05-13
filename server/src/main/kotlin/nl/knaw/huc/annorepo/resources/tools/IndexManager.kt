package nl.knaw.huc.annorepo.resources.tools

import java.util.UUID
import com.mongodb.client.model.Indexes
import nl.knaw.huc.annorepo.api.ARConst
import nl.knaw.huc.annorepo.api.IndexChoreIndex
import nl.knaw.huc.annorepo.api.IndexType
import nl.knaw.huc.annorepo.api.IndexType.ASCENDING
import nl.knaw.huc.annorepo.api.IndexType.DESCENDING
import nl.knaw.huc.annorepo.api.IndexType.HASHED
import nl.knaw.huc.annorepo.api.IndexType.TEXT
import nl.knaw.huc.annorepo.dao.ContainerDAO

class IndexManager(val containerDAO: ContainerDAO) {
    data class IndexPart(
        val fieldName: String,
        val indexTypeName: String,
        val indexType: IndexType,
        val isJsonField: Boolean = true
    )

    fun startIndexCreation(
        containerName: String,
        indexParts: List<IndexPart>
    ): IndexChore {
        val container = containerDAO.getCollection(containerName)
        val fullFieldNames = mutableListOf<String>()
        val indexes = indexParts.map {
            val fullFieldName = if (it.isJsonField) "${ARConst.ANNOTATION_FIELD}.${it.fieldName}" else it.fieldName
            fullFieldNames.add(fullFieldName)
            when (it.indexType) {
                HASHED -> Indexes.hashed(fullFieldName)
                ASCENDING -> Indexes.ascending(fullFieldName)
                DESCENDING -> Indexes.descending(fullFieldName)
                TEXT -> Indexes.text(fullFieldName)
//                else -> throw RuntimeException("Cannot make an index with type $it.indexType on field $fullFieldName")
            }
        }
        val index = Indexes.compoundIndex(indexes)
        val id = UUID.randomUUID().toString()
        return startIndexChore(
            IndexChore(
                id = id,
                container = container,
                containerName = containerName,
                fieldNames = fullFieldNames,
                index = index,
                containerDAO = containerDAO
            )
        )
    }

    fun getIndexChore(indexName: String): IndexChore? = IndexChoreIndex[indexName]

    private fun startIndexChore(chore: IndexChore): IndexChore {
        IndexChoreIndex[chore.id] = chore
        Thread(chore).start()
        return chore
    }

    companion object {
        fun List<IndexPart>.toIndexName(): String =
            joinToString("/") { "${it.fieldName}/${it.indexType}" }
    }

}