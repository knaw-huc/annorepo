package nl.knaw.huc.annorepo.exceptions

private const val ETAG_MISMATCH = "Etag does not match"

class PreconditionFailedException : RuntimeException(ETAG_MISMATCH)