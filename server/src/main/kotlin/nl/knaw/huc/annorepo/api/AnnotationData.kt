package nl.knaw.huc.annorepo.api

import java.beans.ConstructorProperties
import java.util.Date

class AnnotationData @ConstructorProperties("id", "name") constructor(
    var id: Long,
    name: String,
    content: String,
    created: Date,
    modified: Date
) {
    var name: String? = name
    var content: String? = content
    var created: Date? = created
    var modified: Date? = modified

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AnnotationData

        if (id != other.id) return false
        if (name != other.name) return false
        if (content != other.content) return false
        if (created != other.created) return false
        return modified == other.modified
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