import com.fasterxml.jackson.databind.ObjectMapper
import com.mongodb.client.MongoClients
import com.mongodb.client.MongoCollection
import com.mongodb.client.model.Aggregates.limit
import com.mongodb.client.model.Aggregates.match
import com.mongodb.client.model.Aggregates.skip
import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Filters.gte
import com.mongodb.client.model.Filters.lte
import org.assertj.core.api.Assertions.assertThat
import org.bson.Document
import org.junit.jupiter.api.Test
import org.litote.kmongo.aggregate

class MongodbTest {

    @Test
    fun testMongodb1() {
        logger.info("a")
        MongoClients.create("mongodb://localhost/").use { client ->
            logger.info("a1")
            val database = client.getDatabase("annorepo")
            val collection = database.getCollection("a84f3029-a222-4b79-a3a1-fadf24d08aad")
            val a = collection.find(Document(ANNOTATION_NAME_FIELD, "318df1ca-6e8c-4430-bcda-78f838e30a2b"))
            logger.info("a=$a")
            for (i in a) {
                logger.info("annotation=${i.toJson()}")
            }
        }
        logger.info("b")
    }

    @Test
    fun testMongodb() {
        MongoClients.create("mongodb://localhost/").use { client ->
            val database = client.getDatabase("annorepo")
            val rcResult = database.runCommand(Document("dbStats", 1).append("scale", 1024))
            logger.info("rcResult = $rcResult")

            val toys: MongoCollection<Document> = database.getCollection("annotation")

            val toy = Document("name", "yoyo").append("ages", Document("min", 5))
            logger.info("toy=$toy")

            val id = toys.insertOne(toy).insertedId?.asObjectId()?.value
            logger.info("id=$id")

            val yoyo = toys.find(Document("name", "yoyo")).first()
            logger.info("$yoyo")

            val toy2 = Document("name", "yoyo").append("ages", "9-12")
            val id2 = toys.insertOne(toy2).insertedId?.asObjectId()?.value
            logger.info("id2=$id2")

            val result = toys.find(Document("name", "yoyo"))
            result.forEach { a ->
                val j = ObjectMapper().writeValueAsString(a)
                logger.info(j)
            }

            val json = """
                {
        "@context": "http://www.w3.org/ns/anno.jsonld",
        "id": "urn:example:republic:annotation:df11d729-4866-4d44-b627-2c4157322177",
        "type": "Annotation",
        "motivation": "classifying",
        "generated": "2021-11-18T11:15:58.186555",
        "generator": {
            "id": "https://github.com/knaw-huc/un-t-ann-gle",
            "type": "Software",
            "name": "un-t-ann-gle"
        },
        "body": {
            "type": "TextualBody",
            "purpose": "classifying",
            "value": "line",
            "id": "urn:example:republic:NL-HaNA_1.01.02_3783_0285-page-568-column-1-tr-2-line-0"
        },
        "target": [
            {
                "source": "https://demorepo.tt.di.huc.knaw.nl/task/find/volume-1728/file/contents?type=anchor",
                "type": "Text",
                "selector": {
                    "type": "urn:example:republic:TextAnchorSelector",
                    "start": 55952,
                    "end": 55952
                }
            },
            {
                "source": "https://images.diginfra.net/iiif/NL-HaNA_1.01.02/3783/NL-HaNA_1.01.02_3783_0285.jpg/full/,3100/0/default.jpg",
                "type": "Image",
                "selector": {
                    "type": "FragmentSelector",
                    "conformsTo": "http://www.w3.org/TR/media-frags/",
                    "value": "xywh=1451,2124,511,60"
                }
            },
            {
                "source": "https://images.diginfra.net/iiif/NL-HaNA_1.01.02/3783/NL-HaNA_1.01.02_3783_0285.jpg/full/,1316/0/default.jpg",
                "type": "Image",
                "selector": {
                    "type": "FragmentSelector",
                    "conformsTo": "http://www.w3.org/TR/media-frags/",
                    "value": "xywh=1451,2124,511,60"
                }
            },
            {
                "source": "https://images.diginfra.net/iiif/NL-HaNA_1.01.02/3783/NL-HaNA_1.01.02_3783_0285.jpg/1451,2124,511,60/full/0/default.jpg",
                "type": "Image"
            }
        ]
    }
            """.trimIndent()
            val doc = Document.parse(json)
            val r = toys.insertOne(doc).insertedId?.asObjectId()?.value
            toys.find(Document("_id", r)).forEach { i -> logger.info(i.toJson()) }

            val toyList = listOf(
                Document("type", "Annotation").append("body", Document("id", "iri:some-iri")),
                Document("type", "Annotation").append("body", "iri"),
                Document("type", "Annotation").append("body", Document("value", "iri:some-iri")),
                Document("type", "Annotation").append(
                    "body", Document("value", Document("amount", 12).append("unit", "m"))
                ),
                Document("type", "Annotation").append("body", Document("id", "iri:some-other-iri")),
            )
            toys.insertMany(toyList)

            toys.find(Document("type", "Annotation")).forEach { a ->
                val j = ObjectMapper().writeValueAsString(a)
                logger.info(j)
            }
            logger.info("aggregate")
            toys.aggregate(
                listOf(
                    match(eq("body.value", "iri:some-iri")),
                    match(eq("type", "Annotation")),
                )
            ).forEach { a ->
                val j = ObjectMapper().writeValueAsString(a)
                logger.info(j)
            }

            logger.info("range overlap filter")

        }
    }

    @Test
    fun testSearchByRange() {
        val collectionName = "searchbyrange"
        MongoClients.create("mongodb://localhost/").use { client ->
            val database = client.getDatabase("annorepo")
            val collection: MongoCollection<Document> = database.getCollection(collectionName)
            collection.drop()
            logger.info("$collectionName contains ${collection.countDocuments()} documents.")

            // setup collection
            val targetSource = "urn:textrepo:text_x"
            val selectorType = """urn:example:republic:TextAnchorSelector"""
            for (n in 0..10) {
                val annotationJson = """
                {
                    "@context": "http://www.w3.org/ns/anno.jsonld",
                    "id": "urn:example:republic:annotation:$n",
                    "type": "Annotation",
                    "motivation": "classifying",
                    "body": {
                        "type": "TextualBody",
                        "purpose": "classifying",
                        "value": "line",
                        "id": "urn:example:republic:NL-HaNA_1.01.02_3783_0285-page-568-column-1-tr-2-line-$n"
                    },
                    "target": [
                        {
                            "source": "$targetSource",
                            "type": "Text",
                            "selector": {
                                "type": "$selectorType",
                                "start": ${n * 80},
                                "end": ${(n + 1) * 80 - 1}
                            }
                        }
                    ]
                }""".trimIndent()
                val annotationDoc = Document.parse(annotationJson)
                collection.insertOne(Document(ANNOTATION_NAME_FIELD, "A$n").append(ANNOTATION_FIELD, annotationDoc))
            }
            logger.info("$collectionName contains ${collection.countDocuments()} documents.")

            // query collection
            val results = collection.aggregate<Document>(
                match(
                    and(
                        eq("annotation.target.source", targetSource),
                        eq("annotation.target.selector.type", selectorType),
                    )
                )
            ).toList()
            assertThat(results.size).isEqualTo(11)

            val pageSize = 2
            val offset = 1
            val results2 = collection.aggregate<Document>(
                match(
                    and(
                        eq("annotation.target.source", targetSource),
                        eq("annotation.target.selector.type", selectorType),
                        gte("annotation.target.selector.start", 100),
                        lte("annotation.target.selector.end", 400),
                    )
                ),
                skip(offset),
                limit(pageSize),
            ).toList()
//            results2.forEach {
//                println(it.getEmbedded(listOf("annotation", "body", "id"), String::class.java))
//                println(
//                    it.getList("target", Document::class.java)
//                        .joinToString { d ->
//                            d.getEmbedded(
//                                listOf("selector", "start"), Integer::class.java
//                            ).toString() +
//                                    "-" +
//                                    d.getEmbedded(listOf("selector", "end"), Integer::class.java)
//                        })
//            }
            assertThat(results2.size).isEqualTo(3)

            // cleanup collection
//            collection.drop()
            logger.info("$collectionName contains ${collection.countDocuments()} documents.")
        }
    }
}