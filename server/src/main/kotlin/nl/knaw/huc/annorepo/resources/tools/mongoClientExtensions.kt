package nl.knaw.huc.annorepo.resources.tools

import com.mongodb.client.MongoClient
import org.bson.Document

fun MongoClient.getMongoVersion(): String =
    getDatabase("admin")
        .runCommand(Document("buildInfo", 1))
        .getString("version")
