package nl.knaw.huc.annorepo.tasks

import com.codahale.metrics.annotation.Metered
import com.google.common.collect.SortedMultiset
import com.google.common.collect.TreeMultiset
import com.mongodb.client.MongoClient
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Filters.exists
import io.dropwizard.servlets.tasks.Task
import nl.knaw.huc.annorepo.api.ARConst.CONTAINER_METADATA_COLLECTION
import nl.knaw.huc.annorepo.api.ContainerMetadata
import nl.knaw.huc.annorepo.config.AnnoRepoConfiguration
import nl.knaw.huc.annorepo.service.JsonLdUtils
import org.litote.kmongo.findOne
import org.litote.kmongo.getCollection
import org.litote.kmongo.json
import java.io.PrintWriter
import javax.ws.rs.BadRequestException

class RecalculateFieldCountTask(
    val client: MongoClient,
    configuration: AnnoRepoConfiguration
) : Task("recalculate-field-count") {
    private val mdb = client.getDatabase(configuration.databaseName)

    @Metered
    override fun execute(parameters: MutableMap<String, MutableList<String>>, output: PrintWriter) {
        if (parameters.contains("container")) {
            val containerName = parameters["container"]!![0]
            checkContainerExists(containerName)
            recalculateFieldCount(output, containerName)

        } else {
            output.println("Recalculating the field count for all containers:")
            output.flush()
            val containerNames = mdb.listCollectionNames()
                .filter { name -> !name.startsWith("_") }
                .sorted()
            for (containerName in containerNames) {
                recalculateFieldCount(output, containerName)
            }
        }
        output.println("Done!")
    }

    private fun recalculateFieldCount(output: PrintWriter, containerName: String) {
        output.println("Recalculating the field count for container $containerName")
        output.flush()
        val container = mdb.getCollection(containerName)
        val fields = container.find(exists("annotation"))
            .flatMap { d -> JsonLdUtils.extractFields(d["annotation"]!!.json) }
            .filter { f -> !f.contains("@") }
            .toList()
        val bag: SortedMultiset<String> = TreeMultiset.create()
        for (f in fields) {
            bag.add(f)
        }
        val fieldCounts = mutableMapOf<String, Int>()
        for (e in bag.entrySet()) {
            fieldCounts[e.element] = e.count
        }

        val containerMetadataCollection = mdb.getCollection<ContainerMetadata>(CONTAINER_METADATA_COLLECTION)
        val containerMetadata: ContainerMetadata =
            containerMetadataCollection.findOne(eq("name", containerName)) ?: return
        val newContainerMetadata = containerMetadata.copy(fieldCounts = fieldCounts)
        containerMetadataCollection.replaceOne(eq("name", containerName), newContainerMetadata)
    }

    private fun checkContainerExists(containerName: String) {
        if (!mdb.listCollectionNames().contains(containerName)) {
            throw BadRequestException("Annotation Container '$containerName' not found")
        }
    }

}