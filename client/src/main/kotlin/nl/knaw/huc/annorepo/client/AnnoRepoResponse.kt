package nl.knaw.huc.annorepo.client

data class AnnoRepoResponse(val created: Boolean, val location: String, val containerId: String, val eTag: String)
