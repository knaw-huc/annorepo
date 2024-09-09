package nl.knaw.huc.annorepo.resources.tools

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

    //    fun startIndexCreation(
//        containerName: String,
//        indexParts: List<ContainerServiceResource.IndexPart>
//    ): IndexChore {
//        val container = containerDAO.getCollection(containerName)
//        val fullFieldName = if (isJsonField) "${ARConst.ANNOTATION_FIELD}.${fieldName}" else fieldName
//        Indexes.
//        val index = when (indexType) {
//            HASHED -> Indexes.hashed(fullFieldName)
//            ASCENDING -> Indexes.ascending(fullFieldName)
//            DESCENDING -> Indexes.descending(fullFieldName)
//            TEXT -> Indexes.text(fieldName)
//            else -> throw RuntimeException("Cannot make an index with type $indexType")
//        }
//        return startIndexChore(
//            IndexChore(
//                id = choreId(containerName, fieldName, indexTypeName),
//                container = container,
//                fieldName = fullFieldName,
//                index = index
//            )
//        )
//    }
    fun startIndexCreation(
        containerName: String,
        fieldName: String,
        indexTypeName: String,
        indexType: IndexType,
        isJsonField: Boolean = true
    ): IndexChore {
        val container = containerDAO.getCollection(containerName)
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