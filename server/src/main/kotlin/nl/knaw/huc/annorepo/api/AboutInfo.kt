package nl.knaw.huc.annorepo.api

data class AboutInfo(
    val appName: String,
    val version: String,
    val startedAt: String,
    val baseURI: String,
    val source: String = "https://github.com/knaw-huc/annorepo",
    val needsAuthentication: Boolean
)