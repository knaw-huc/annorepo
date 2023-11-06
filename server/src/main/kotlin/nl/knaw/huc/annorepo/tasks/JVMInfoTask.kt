package nl.knaw.huc.annorepo.tasks

import java.io.File
import java.io.PrintWriter
import com.codahale.metrics.annotation.Metered
import io.dropwizard.servlets.tasks.Task
import nl.knaw.huc.annorepo.resources.tools.formatAsSize

class JVMInfoTask : Task("jvm-info") {

    @Metered
    override fun execute(parameters: MutableMap<String, MutableList<String>>, output: PrintWriter) {
        val runtime = Runtime.getRuntime()
        output.println("Available processors (cores): " + runtime.availableProcessors())

        val heapSpace = runtime.totalMemory().formatAsSize
        val freeMemory = runtime.freeMemory().formatAsSize
        val maxMemory = runtime.maxMemory()

        val maximum = if (maxMemory == Long.MAX_VALUE) "no limit" else maxMemory.formatAsSize
        output.println("Heap space:     $heapSpace")
        output.println("Free memory:    $freeMemory")
        output.println("Maximum memory: $maximum")

        output.println()

        for (root in File.listRoots()) {
            output.println("File system root: ${root.absolutePath}")
            output.println("Total space:  ${root.totalSpace.formatAsSize}")
            output.println("Free space:   ${root.freeSpace.formatAsSize}")
            output.println("Usable space: ${root.usableSpace.formatAsSize}")
        }
    }

}