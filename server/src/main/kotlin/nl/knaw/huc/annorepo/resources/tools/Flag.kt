package nl.knaw.huc.annorepo.resources.tools

class Flag(param: String?) {
    val isPresent: Boolean = param != null
}