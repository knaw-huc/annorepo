package nl.knaw.huc.annorepo.tasks

import java.io.File
import java.io.PrintWriter
import com.codahale.metrics.annotation.Metered
import io.dropwizard.servlets.tasks.Task
import nl.knaw.huc.annorepo.resources.tools.formatAsSize

class JVMInfoTask(
) : Task("jvm-info") {

    @Metered
    override fun execute(parameters: MutableMap<String, MutableList<String>>, output: PrintWriter) {
        output.println("Available processors (cores): " + Runtime.getRuntime().availableProcessors())

        val heapSpace = Runtime.getRuntime().totalMemory().formatAsSize
        val freeMemory = Runtime.getRuntime().freeMemory().formatAsSize
        val maxMemory = Runtime.getRuntime().maxMemory()

        val maximum = if (maxMemory == Long.MAX_VALUE) "no limit" else maxMemory.formatAsSize
        output.println("Heap space:     $heapSpace")
        output.println("Free memory:    $freeMemory")
        output.println("Maximum memory: $maximum")

        output.println()

        /* For each filesystem root, print some info */
        for (root in File.listRoots()) {
            output.println("File system root: ${root.absolutePath}")
            output.println("Total space:  ${root.totalSpace.formatAsSize}")
            output.println("Free space:   ${root.freeSpace.formatAsSize}")
            output.println("Usable space: ${root.usableSpace.formatAsSize}")
        }
    }

}