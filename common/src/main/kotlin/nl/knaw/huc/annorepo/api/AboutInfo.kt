package nl.knaw.huc.annorepo.api

data class AboutInfo(
    val appName: String,
    val version: String,
    val startedAt: String,
    val baseURI: String,
    val withAuthentication: Boolean,
    val sourceCode: String = "https://github.com/knaw-huc/annorepo"
)