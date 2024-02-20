package nl.knaw.huc.annorepo

import java.io.StringReader
import jakarta.json.Json
import jakarta.json.JsonValue
import org.junit.jupiter.api.Test
import com.mongodb.client.MongoDatabase
import org.apache.logging.log4j.kotlin.logger
import org.assertj.core.api.Assertions.assertThat
import org.litote.kmongo.KMongo
import nl.knaw.huc.annorepo.api.ARConst
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

    @Test
    fun test() {
        val containerName = "republic"
        val queryJson = """{"body.id":"urn:republic:NL-HaNA_1.01.02_3783_0051-page-101"}"""
        val queryMap: Map<String, Any?> = Json.createReader(StringReader(queryJson)).readObject().toMap().simplify()
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
        logger.info { "o=$o" }
        val s = o.simplify()
        logger.info { "s=$s" }
    }

    @Test
    fun testGetDistinctValues() {
        val containerDAO = ARContainerDAO(configuration, mongoClient)
        val distinctValues = containerDAO.getDistinctValues("large-container", "body.type")
        logger.info { distinctValues }
        val cachedDistinctValues = containerDAO.getDistinctValues("large-container", "body.type")
        logger.info { cachedDistinctValues }
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
            logger.info { "container $cn" }
            val collection = containerDAO.getCollection(cn)
            val indexes = collection.listIndexes().toList()
            logger.info { "  indexes:" }
            for (i in indexes) {
                logger.info { "  - ${i["name"]}" }
            }
            val names =
                collection.listIndexes().filterNotNull().map { it["name"] }
            val hasAnnotationNameIndex = collection.hasAnnotationNameIndex()
            logger.info { "names=$names" }
            logger.info { hasAnnotationNameIndex }
        }
    }

    private fun Map<String, JsonValue>.simplify(): Map<String, Any?> {
        val newMap = mutableMapOf<String, Any?>()
        for (e in entries) {
            logger.info { "e=$e" }
            val v = e.value
            logger.info { "v=$v" }
            newMap[e.key] = v.toSimpleValue()
        }

        return newMap
    }

}