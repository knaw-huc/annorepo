package nl.knaw.huc.annorepo.client

import arrow.core.Either
import io.grpc.Metadata

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

internal fun Metadata.setAsciiKey(key: String, value: String) {
    put(Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER), value)
}
