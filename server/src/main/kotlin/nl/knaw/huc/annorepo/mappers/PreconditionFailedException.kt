package nl.knaw.huc.annorepo.mappers

private const val ETAG_MISMATCH = "Etag does not match"

class PreconditionFailedException : RuntimeException(ETAG_MISMATCH)