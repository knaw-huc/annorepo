package nl.knaw.huc.annorepo.client

import arrow.core.Either

fun Sequence<Either<RequestError, String>>.untangled(): Either<RequestError, List<String>> {
    val list: MutableList<String> = mutableListOf()
    var error: RequestError? = null
    forEach { e ->
        when (e) {
            is Either.Left -> error = e.value
            is Either.Right -> list.add(e.value)
        }
    }
    return if (error == null) {
        Either.Right(list.toList())
    } else {
        Either.Left(error!!)
    }
}
