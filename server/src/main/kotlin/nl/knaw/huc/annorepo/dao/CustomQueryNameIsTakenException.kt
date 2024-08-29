package nl.knaw.huc.annorepo.dao

class CustomQueryNameIsTakenException(name: String) :
    Throwable(message = "Duplicate custom query name: $name (there already exists a custom query with that name)")
