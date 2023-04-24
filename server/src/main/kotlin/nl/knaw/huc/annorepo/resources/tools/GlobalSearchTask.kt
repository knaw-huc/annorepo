package nl.knaw.huc.annorepo.resources.tools

import com.mongodb.client.MongoClient
import com.mongodb.client.MongoDatabase
import org.bson.Document
import org.bson.conversions.Bson
import nl.knaw.huc.annorepo.api.ARConst
import nl.knaw.huc.annorepo.config.AnnoRepoConfiguration
import nl.knaw.huc.annorepo.service.UriFactory

class GlobalSearchTask(
    private val containerNames: List<String>,
    queryMap: HashMap<*, *>,
    private val aggregateStages: List<Bson>,
    configuration: AnnoRepoConfiguration,
    client: MongoClient
) : SearchTask(queryMap) {
    //    private val log = LoggerFactory.getLogger(javaClass)
    private val uriFactory = UriFactory(configuration)
    val mdb: MongoDatabase = client.getDatabase(configuration.databaseName)

    override fun runSearch(status: Status) {
        status.totalContainersToSearch = containerNames.size
        for (containerName in containerNames) {
            mdb.getCollection(containerName)
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
                    "id", uriFactory.annotationURL(containerName, a.getString(ARConst.ANNOTATION_NAME_FIELD))
                )
            }
}