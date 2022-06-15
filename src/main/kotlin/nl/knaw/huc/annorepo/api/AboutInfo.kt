package nl.knaw.huc.annorepo.api

data class AboutInfo(
    val appName: String,
    val version: String,
    val startedAt: String,
    val baseURI: String
)