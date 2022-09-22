package nl.knaw.huc.annorepo.tasks

import com.codahale.metrics.annotation.Metered
import com.mongodb.client.MongoClient
import io.dropwizard.servlets.tasks.Task
import nl.knaw.huc.annorepo.config.AnnoRepoConfiguration
import java.io.PrintWriter

class UpdateTask(
    val client: MongoClient,
    configuration: AnnoRepoConfiguration
) : Task("update") {
    private val mdb = client.getDatabase(configuration.databaseName)

    @Metered
    override fun execute(parameters: MutableMap<String, MutableList<String>>, output: PrintWriter) {
        output.println("no update actions required!")
    }

}