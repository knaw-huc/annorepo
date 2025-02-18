package nl.knaw.huc.annorepo.resources.tools

import com.mongodb.kotlin.client.MongoClient
import com.mongodb.kotlin.client.MongoCursor
import org.apache.logging.log4j.kotlin.logger
import org.bson.Document

fun MongoClient.getMongoVersion(): String =
    getDatabase("admin")
        .runCommand(Document("buildInfo", 1))
        .getString("version")

fun <TResult> MongoCursor<TResult>.isOpenAndHasNext(): Boolean {
    // because calling hasNext() on closed cursor throws an exception ?!
    try {
        return this.hasNext()
    } catch (e: IllegalStateException) {
        logger.warn { "cursor threw an IllegalStateException: ${e.message}" }
        return false
    }
}

fun <TResult> MongoCursor<TResult>.isClosed(): Boolean {
    try {
        this.hasNext()
        return false
    } catch (e: IllegalStateException) {
        return true
    }
}
