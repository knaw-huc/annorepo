package nl.knaw.huc.annorepo.integration

class MyBool(private val initialValue: Boolean) {
    var value: Boolean = initialValue

    fun orWith(bool: Boolean) {
        value = value || bool
    }

    fun andWith(bool: Boolean) {
        value = value && bool
    }

    fun not() {
        value = !value
    }
}