package nl.knaw.huc.annorepo.resources

data class ESIndexBulkOperation(
    val index: String,
    val annotationId: String,
    val annotationName: String,
    val annotationJson: String
)
