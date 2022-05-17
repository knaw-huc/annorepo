package nl.knaw.huc.annorepo.api

import co.elastic.clients.elasticsearch.ElasticsearchClient
import co.elastic.clients.elasticsearch._types.ElasticsearchException
import co.elastic.clients.elasticsearch._types.mapping.Property
import co.elastic.clients.elasticsearch.core.BulkResponse
import co.elastic.clients.elasticsearch.core.IndexRequest
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation
import co.elastic.clients.elasticsearch.core.bulk.IndexOperation
import co.elastic.clients.json.JsonData
import com.fasterxml.jackson.databind.ObjectMapper
import nl.knaw.huc.annorepo.resources.ESIndexBulkOperation
import org.slf4j.LoggerFactory
import java.io.Reader
import java.io.StringReader

class ElasticsearchWrapper(private val esClient: ElasticsearchClient) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun createIndex(name: String): Boolean {
        val response = esClient.indices().create { _1 ->
            _1.index(name).mappings { _2 ->
                _2.properties(mapOf("annotation_name" to Property.of {
                    it.keyword { it1 -> it1 }
                }, "annotation" to Property.of { _3 ->
                    _3.`object` { it }
//                    _3.`object` { _4 ->
//                        _4.properties(
//                            mapOf(
//                                "@context" to Property.of { _5 ->
//                                    _5.`object` { it.enabled(false) }
//                                },
//                                "id" to Property.of { _5 ->
//                                    _5.keyword { it }
//                                },
//                                "type" to Property.of { _5 ->
//                                    _5.keyword { it }
//                                },
//                                "created" to Property.of { _5 ->
//                                    _5.`date` { it }
//                                },
//                                "generator" to Property.of { _5 ->
//                                    _5.`object` { it }
//                                },
//                                "body" to Property.of { _5 ->
//                                    _5.`object` { it }
//                                },
//                                "target" to Property.of { _5 ->
//                                    _5.`object` { it }
//                                }
//                            )
//                        )
//                    }
                }))
            }
        }
        return response.acknowledged() && response.shardsAcknowledged()
    }

    fun deleteIndex(containerName: String): Boolean =
        esClient.indices().delete { it.index(containerName) }.acknowledged()

    fun indexAnnotation(dbId: Long, containerName: String, name: String, annotationJson: String) {
        val wrapperJson = wrapJson(name, annotationJson)
        val request = IndexRequest.of { i: IndexRequest.Builder<JsonData> ->
            i.index(containerName).id(dbId.toString()).withJson(StringReader(wrapperJson))
        }
        val result = esClient.index(request).result()
        log.debug("result = $result")
    }

    private fun wrapJson(name: String, annotationJson: String): String =
        """{
        |"annotation_name":"$name",
        |"annotation":$annotationJson
        |}""".trimMargin()

    fun deindexAnnotation(containerName: String, annotationId: Long) {
        val result = esClient.delete { it.index(containerName).id(annotationId.toString()) }.result()
        log.debug("result=$result")
    }

    data class BulkIndexResult(val success: Boolean, val errors: Map<String, String>)

    //    Since esClient.bulk() doesn't seem to work with JsonData, let's try it sequentially'
    fun bulkIndex1(bulkOperations: List<ESIndexBulkOperation>): BulkIndexResult {
        val errorMap = mutableMapOf<String, String>()
        for (ibo: ESIndexBulkOperation in bulkOperations) {
            try {
                val reader: Reader = StringReader(wrapJson(ibo.annotationName, ibo.annotationJson))
                val result = esClient.index(
                    IndexRequest.Builder<JsonData>()
                        .index(ibo.index)
                        .id(ibo.annotationId)
                        .withJson(reader).build()
                )
                log.info("result=${result.result()}")
            } catch (exception: ElasticsearchException) {
                log.error("exception: $exception")
                errorMap[ibo.annotationId] = exception.message.toString()
            }
        }
        val success = errorMap.isEmpty()
        return BulkIndexResult(success = success, errors = errorMap)
    }

    fun bulkIndex(bulkOperations: List<ESIndexBulkOperation>): BulkIndexResult {
        try {
            val list: List<BulkOperation> = bulkOperations.map { ipo ->
                val wrapJson = wrapJson(ipo.annotationName, ipo.annotationJson)
                val obj = ObjectMapper().readTree(wrapJson)
                BulkOperation.Builder().index { b: IndexOperation.Builder<Any> ->
                    b.index(ipo.index)
                        .id(ipo.annotationId)
                        .document(obj)
                }.build()
            }

            val result: BulkResponse = esClient.bulk { it.operations(list) }

            if (result.errors()) {
                log.error("Bulk had errors")
                for (item in result.items()) {
                    if (item.error() != null) {
                        log.error(item.error()!!.reason())
                    }
                }
            }
        } catch (e: Exception) {
            log.error("$e")
            throw e
        }
        return BulkIndexResult(success = true, errors = mapOf())
    }

}
