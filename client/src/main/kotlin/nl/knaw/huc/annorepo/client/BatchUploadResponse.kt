package nl.knaw.huc.annorepo.client

import nl.knaw.huc.annorepo.api.AnnotationIdentifier

data class BatchUploadResponse(val annotationData: List<AnnotationIdentifier>)
