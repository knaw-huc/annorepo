package nl.knaw.huc.annorepo.resources.tools

import org.bson.Document
import org.bson.conversions.Bson
import nl.knaw.huc.annorepo.api.ARConst

class GlobalSearchTask(
    private val containerNames: List<String>,
    queryMap: HashMap<*, *>,
    private val aggregateStages: List<Bson>,
    private val context: SearchManager.Context
) : SearchTask(queryMap) {
    override fun runSearch(status: Status) {
        status.totalContainersToSearch = containerNames.size
        for (containerName in containerNames) {
            context.mdb.getCollection(containerName)
                .aggregate(aggregateStages)
                .map { a -> toAnnotationMap(a, containerName) }
                .forEach(status.annotations::add)
            status.containersSearched.incrementAndGet()
        }
    }

    private fun toAnnotationMap(a: Document, containerName: String): Map<String, Any> =
        a.get(ARConst.ANNOTATION_FIELD, Document::class.java)
            .toMutableMap()
            .apply<MutableMap<String, Any>> {
                put(
                    "id", context.uriFactory.annotationURL(containerName, a.getString(ARConst.ANNOTATION_NAME_FIELD))
                )
            }
}