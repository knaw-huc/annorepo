package nl.knaw.huc.annorepo.resources.tools

import java.io.InputStream
import java.io.InputStreamReader
import java.io.Reader

object ResourceLoader {
    fun asStream(path: String): InputStream? {
        return Thread.currentThread().contextClassLoader.getResourceAsStream(path)
    }

    fun asReader(path: String): Reader? {
        return asStream(path)?.let { InputStreamReader(it) }
    }

    fun asText(path: String): String? {
        return Thread.currentThread().contextClassLoader.getResource(path)?.readText()
    }
}