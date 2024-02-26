package nl.knaw.huc.annorepo

import java.io.StringReader
import jakarta.json.Json
import jakarta.json.JsonValue
import org.junit.jupiter.api.Test
import com.mongodb.client.MongoDatabase
import org.assertj.core.api.Assertions.assertThat
import org.litote.kmongo.KMongo
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import nl.knaw.huc.annorepo.api.ARConst
import nl.knaw.huc.annorepo.api.PropertySet
import nl.knaw.huc.annorepo.config.AnnoRepoConfiguration
import nl.knaw.huc.annorepo.dao.ARContainerDAO
import nl.knaw.huc.annorepo.resources.tools.AggregateStageGenerator
import nl.knaw.huc.annorepo.resources.tools.hasAnnotationNameIndex
import nl.knaw.huc.annorepo.resources.tools.toSimpleValue

class MongoTester {
    private val mongoClient = KMongo.createClient("mongodb://localhost/")
    private val mdb: MongoDatabase = mongoClient.getDatabase("annorepo")
    private val configuration = AnnoRepoConfiguration()
    private val aggregateStageGenerator = AggregateStageGenerator(configuration)
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    @Test
    fun test() {
        val containerName = "republic"
        val queryJson = """{"body.id":"urn:republic:NL-HaNA_1.01.02_3783_0051-page-101"}"""
        val queryMap: PropertySet = Json.createReader(StringReader(queryJson)).readObject().toMap().simplify()
//        val queryMap: Map<String, Any?> = mapOf("body.id" to "urn:republic:NL-HaNA_1.01.02_3783_0051-page-101")
        println(queryMap)

        val container = mdb.getCollection(containerName)
        val aggregateStages = queryMap
            .map { (k, v) -> aggregateStageGenerator.generateStage(k, v!!) }
            .toList()
        val count = container.aggregate(aggregateStages).count()
        println("!!count:$count")
        val annotations =
            mdb.getCollection(containerName)
                .aggregate(aggregateStages)
                .toList()
        println("!!annotations=${annotations}")

    }

    @Test
    fun test2() {
        val string = """{"string":"v"}"""
        val o = Json.createReader(StringReader(string)).readObject().toMap()
        log.info("o={}", o)
        val s = o.simplify()
        log.info("s={}", s)
    }

    @Test
    fun testGetDistinctValues() {
        val containerDAO = ARContainerDAO(configuration, mongoClient)
        val distinctValues = containerDAO.getDistinctValues("large-container", "body.type")
        log.info("{}", distinctValues)
        val cachedDistinctValues = containerDAO.getDistinctValues("large-container", "body.type")
        log.info("{}", cachedDistinctValues)
        assertThat(cachedDistinctValues).isEqualTo(distinctValues)
    }

    private fun allAnnotationContainers(): List<String> = mdb.listCollectionNames()
        .filter { it != ARConst.CONTAINER_METADATA_COLLECTION }
        .filter { !it.startsWith('_') }
        .sorted<String>()
        .toList()

    @Test
    fun testIndexes() {
        val containerDAO = ARContainerDAO(configuration, mongoClient)
        val allContainers = allAnnotationContainers()
        for (cn in allContainers) {
            log.info("container {}", cn)
            val collection = containerDAO.getCollection(cn)
            val indexes = collection.listIndexes().toList()
            log.info("  indexes:")
            for (i in indexes) {
                log.info("  - {}", i["name"])
            }
            val names =
                collection.listIndexes().filterNotNull().map { it["name"] }
            val hasAnnotationNameIndex = collection.hasAnnotationNameIndex()
            log.info("names={}", names)
            log.info("{}", hasAnnotationNameIndex)
        }
    }

    private fun Map<String, JsonValue>.simplify(): PropertySet {
        val newMap = mutableMapOf<String, Any?>()
        for (e in entries) {
            log.info("e={}", e)
            val v = e.value
            log.info("v={}", v)
            newMap[e.key] = v.toSimpleValue()
        }

        return newMap
    }

}