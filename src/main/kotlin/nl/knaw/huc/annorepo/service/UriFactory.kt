package nl.knaw.huc.annorepo.service

import nl.knaw.huc.annorepo.api.ResourcePaths
import nl.knaw.huc.annorepo.config.AnnoRepoConfiguration
import java.net.URI
import javax.ws.rs.core.UriBuilder

class UriFactory(private val configuration: AnnoRepoConfiguration) {

    fun containerURL(containerName: String): URI =
        UriBuilder.fromUri(configuration.externalBaseUrl)
            .path(ResourcePaths.W3C)
            .path(containerName)
            .build()

    fun annotationURL(containerName: String, annotationName: String): URI =
        UriBuilder.fromUri(configuration.externalBaseUrl)
            .path(ResourcePaths.W3C)
            .path(containerName)
            .path(annotationName)
            .build()

}