package nl.knaw.huc.annorepo.api

import java.beans.ConstructorProperties
import java.util.*

class AnnotationData {
    var id: Long = 0
    var name: String? = null
    var content: String? = null
    var created: Date? = null
    var modified: Date? = null

    constructor() : super()

    @ConstructorProperties("id", "name")
    constructor(id: Long, name: String, content: String, created: Date, modified: Date) {
        this.id = id
        this.name = name
        this.content = content
        this.created = created
        this.modified = modified
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AnnotationData

        if (id != other.id) return false
        if (name != other.name) return false
        if (content != other.content) return false
        if (created != other.created) return false
        if (modified != other.modified) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + (name?.hashCode() ?: 0)
        result = 31 * result + (content?.hashCode() ?: 0)
        result = 31 * result + (created?.hashCode() ?: 0)
        result = 31 * result + (modified?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "AnnotationData(id=$id, name=$name, content=$content, created=$created, modified=$modified)"
    }

}