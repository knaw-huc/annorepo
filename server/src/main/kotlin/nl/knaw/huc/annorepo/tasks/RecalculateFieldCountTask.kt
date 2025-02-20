package nl.knaw.huc.annorepo.tasks

import java.io.PrintWriter
import jakarta.ws.rs.BadRequestException
import com.codahale.metrics.annotation.Metered
import com.google.common.collect.SortedMultiset
import com.google.common.collect.TreeMultiset
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Filters.exists
import io.dropwizard.servlets.tasks.Task
import org.bson.conversions.Bson
import nl.knaw.huc.annorepo.api.ContainerMetadata
import nl.knaw.huc.annorepo.dao.ContainerDAO
import nl.knaw.huc.annorepo.resources.tools.findOne
import nl.knaw.huc.annorepo.resources.tools.json
import nl.knaw.huc.annorepo.service.JsonLdUtils

class RecalculateFieldCountTask(
    val containerDAO: ContainerDAO
) : Task("recalculate-field-count") {

    @Metered
    override fun execute(parameters: MutableMap<String, MutableList<String>>, output: PrintWriter) {
        if (parameters.contains("container")) {
            val containerName = parameters["container"]!![0]
            checkContainerExists(containerName)
            recalculateFieldCount(output, containerName)

        } else {
            output.println("Recalculating the field count for all containers:")
            output.flush()
            val containerNames = containerDAO.listCollectionNames()
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
        val container = containerDAO.getCollection(containerName)
        val fields = container.find(exists("annotation"))
            .toList()
            .flatMap { d -> JsonLdUtils.extractFields((d["annotation"]!! as Bson).json()) }
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

        val containerMetadataCollection = containerDAO.getContainerMetadataCollection()
        val withContainerName = eq("name", containerName)
        val containerMetadata: ContainerMetadata =
            containerMetadataCollection.findOne(withContainerName) ?: return
        val newContainerMetadata = containerMetadata.copy(fieldCounts = fieldCounts)
        containerMetadataCollection.replaceOne(withContainerName, newContainerMetadata)
    }

    private fun checkContainerExists(containerName: String) {
        if (!containerDAO.listCollectionNames().contains(containerName)) {
            throw BadRequestException("Annotation Container '$containerName' not found")
        }
    }

}
