package nl.knaw.huc.annorepo.dao

interface CustomQueryDAO {
    fun getAllCustomQueries(): List<CustomQuery>
    fun getCustomQuery(name: String): CustomQuery
    fun storeCustomQuery(name: String, query: CustomQuery)
}