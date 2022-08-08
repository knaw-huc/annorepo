package nl.knaw.huc.annorepo.util

import java.io.IOException
import java.util.*
import java.util.jar.JarEntry
import java.util.jar.JarInputStream

/**
 * Reads a library's version if the library contains a Maven pom.properties
 * file. You probably want to cache the output or write it to a constant.
 *
 * @param this@extractVersion any class from the library to check
 * @return the version String, if present
 */
fun Class<*>.extractVersion(): String? {
    val os = protectionDomain?.codeSource?.location?.openStream()
    return if (os != null) {
        JarInputStream(os)
            .readPomProperties(this)?.getProperty("version")
    } else null
}

/**
 * Locate the pom.properties file in the Jar, if present, and return a
 * Properties object representing the properties in that file.
 *
 * @param this@readPomProperties the jar stream to read from
 * @param referenceClass the reference class, whose ClassLoader we'll be
 * using
 * @return the Properties object, if present, otherwise null
 */
private fun JarInputStream.readPomProperties(referenceClass: Class<*>): Properties? {
    try {
        var jarEntry: JarEntry? = nextJarEntry
        while (jarEntry != null) {
            val entryName: String = jarEntry.name
            if (entryName.startsWith("META-INF")
                && entryName.endsWith("pom.properties")
            ) {
                val properties = Properties()
                val classLoader = referenceClass.classLoader
                properties.load(classLoader.getResourceAsStream(entryName))
                return properties
            }
            jarEntry = nextJarEntry
        }
    } catch (ignored: IOException) {
    }
    return null
}
