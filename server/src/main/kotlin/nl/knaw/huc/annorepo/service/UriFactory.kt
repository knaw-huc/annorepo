package nl.knaw.huc.annorepo.service

import java.net.URI
import jakarta.ws.rs.core.UriBuilder
import nl.knaw.huc.annorepo.api.ResourcePaths
import nl.knaw.huc.annorepo.config.AnnoRepoConfiguration

class UriFactory(private val configuration: AnnoRepoConfiguration) {

    fun containerURL(containerName: String): URI =
        UriBuilder.fromUri(configuration.externalBaseUrl)
            .path(ResourcePaths.W3C)
            .path(containerName)
            .path("/")
            .build()

    fun annotationURL(containerName: String, annotationName: String): URI =
        UriBuilder.fromUri(configuration.externalBaseUrl)
            .path(ResourcePaths.W3C)
            .path(containerName)
            .path(annotationName)
            .build()

    fun searchURL(containerName: String, id: String): URI =
        UriBuilder.fromUri(configuration.externalBaseUrl)
            .path(ResourcePaths.CONTAINER_SERVICES)
            .path(containerName)
            .path(ResourcePaths.SEARCH)
            .path(id)
            .build()

    fun searchInfoURL(containerName: String, id: String): URI =
        UriBuilder.fromUri(configuration.externalBaseUrl)
            .path(ResourcePaths.CONTAINER_SERVICES)
            .path(containerName)
            .path(ResourcePaths.SEARCH)
            .path(id)
            .path("info")
            .build()

    fun globalSearchURL(id: String): URI =
        UriBuilder.fromUri(configuration.externalBaseUrl)
            .path(ResourcePaths.GLOBAL_SERVICES)
            .path(ResourcePaths.SEARCH)
            .path(id)
            .build()

    fun globalSearchStatusURL(id: String): URI =
        UriBuilder.fromUri(configuration.externalBaseUrl)
            .path(ResourcePaths.GLOBAL_SERVICES)
            .path(ResourcePaths.SEARCH)
            .path(id)
            .path(ResourcePaths.STATUS)
            .build()

    fun indexURL(containerName: String, fieldName: String, type: String): URI =
        UriBuilder.fromUri(configuration.externalBaseUrl)
            .path(ResourcePaths.CONTAINER_SERVICES)
            .path(containerName)
            .path(ResourcePaths.INDEXES)
            .path(fieldName)
            .path(type.lowercase())
            .build()

    fun indexStatusURL(containerName: String, fieldName: String, type: String): URI =
        UriBuilder.fromUri(configuration.externalBaseUrl)
            .path(ResourcePaths.CONTAINER_SERVICES)
            .path(containerName)
            .path(ResourcePaths.INDEXES)
            .path(fieldName)
            .path(type.lowercase())
            .path(ResourcePaths.STATUS)
            .build()

}