package nl.knaw.huc.annorepo.dao

interface CustomQueryDAO {
    fun getAllCustomQueries(): List<CustomQuery>

    fun nameIsTaken(name: String): Boolean
    fun getByName(name: String): CustomQuery?
    fun store(query: CustomQuery)
    fun deleteByName(customQueryName: String): Boolean
}