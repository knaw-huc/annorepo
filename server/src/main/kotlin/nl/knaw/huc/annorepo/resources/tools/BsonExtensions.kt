package nl.knaw.huc.annorepo.resources.tools

import org.bson.conversions.Bson

object BsonExtensions {
    fun Bson.json(): String = this.toBsonDocument().toJson()
}