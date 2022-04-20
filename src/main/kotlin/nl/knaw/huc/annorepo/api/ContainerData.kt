package nl.knaw.huc.annorepo.api

import java.beans.ConstructorProperties
import java.util.*

class ContainerData {
    var id: Long = 0
    var name: String? = null

    constructor() : super() {}

    @ConstructorProperties("id", "name")
    constructor(id: Long, name: String) {
        this.id = id
        this.name = name
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val container = o as ContainerData
        return id == container.id && name == container.name
    }

    override fun hashCode(): Int {
        return Objects.hash(id, name)
    }
}