package nl.knaw.huc.annorepo.resources.tools

import jakarta.ws.rs.BadRequestException
import kotlin.test.fail
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import net.javacrumbs.jsonunit.assertj.assertThatJson
import org.apache.logging.log4j.kotlin.logger
import org.assertj.core.api.AssertionsForInterfaceTypes.assertThat
import org.assertj.core.api.AssertionsForInterfaceTypes.assertThatExceptionOfType
import org.litote.kmongo.json
import nl.knaw.huc.annorepo.config.AnnoRepoConfiguration

@ExtendWith(MockKExtension::class)
class AggregateStageGeneratorTest {

    @RelaxedMockK
    lateinit var config: AnnoRepoConfiguration

    @Test
    fun `query keys should be strings`() {
//        every { config.rangeSelectorType } returns "something"
        val asg = AggregateStageGenerator(config)
        try {
            asg.generateStage(1, 2)
            fail("expected BadRequestException")
        } catch (bre: BadRequestException) {
            logger.info { (bre.toString()) }
            assertThat(bre.message).isEqualTo("Unexpected field: '1' ; query root fields should be strings")

        }
    }

    @Test
    fun `query keys should not be lists`() {
//        every { config.rangeSelectorType } returns "something"
        val asg = AggregateStageGenerator(config)
        try {
            asg.generateStage(listOf("field1", "field2"), "yes")
            fail("expected BadRequestException")
        } catch (bre: BadRequestException) {
            logger.info(bre.toString())
            assertThat(bre.message)
                .isEqualTo("Unexpected field: '[field1, field2]' ; query root fields should be strings")
        }
    }

    @Test
    fun `query key starting with colon should be a defined query function`() {
        val asg = AggregateStageGenerator(config)
        assertThatExceptionOfType(BadRequestException::class.java)
            .isThrownBy { asg.generateStage(":myQueryFunction", mapOf("parameter1" to "value1")) }
            .withMessage("Unknown query function: ':myQueryFunction'")
    }

    @Test
    fun `query functions - within_range`() {
        val selector = "rangeSelectorType"
        every { config.rangeSelectorType } returns selector
        val asg = AggregateStageGenerator(config)

        val source = "https://google.com"
        val start = 100
        val end = 200
        val parameters = mapOf(
            "source" to source,
            "start" to start,
            "end" to end
        )
        val stage = asg.generateStage(WITHIN_RANGE, parameters)
        logger.info(stage)
        val expected = """
            { 
                "@match": { 
                    "annotation.target" : {
                        "@elemMatch": {
                            "@and": [
                                { "type": "Text"},
                                { "source": "$source"},
                                { "selector.type": "$selector"},
                                { "selector.start": { "@gte": ${start.toFloat()} } },
                                { "selector.end":   { "@lte": ${end.toFloat()} } }
                            ]
                        }
                    }
                }
            }""".trimIndent().replace('@', '$')
        assertThatJson(stage.json).isEqualTo(expected)
        logger.info { stage.json }
    }

    @Test
    fun `simple field matching with string value`() {
        val asg = AggregateStageGenerator(config)
        val key = "body.type"
        val value = "Match"
        val stage = asg.generateStage(key, value)
        logger.info(stage)
        val expected = """
            { "@match": { "annotation.$key": "$value" } }
            """.trimIndent()
            .replace('@', '$')
        assertThatJson(stage.json).isEqualTo(expected)
        logger.info(stage.json)
    }

    @Test
    fun `simple field matching with number value`() {
        val asg = AggregateStageGenerator(config)
        val key = "body.count"
        val value = 42
        val stage = asg.generateStage(key, value)
        logger.info(stage)
        val expected = """
            { "@match": { "annotation.$key": $value } }
            """.trimIndent()
            .replace('@', '$')
        assertThatJson(stage.json).isEqualTo(expected)
        logger.info(stage.json)
    }

    @Test
    fun `special field matching - isNotIn`() {
        val asg = AggregateStageGenerator(config)
        val key = "year"
        val value = mapOf(IS_NOT_IN to arrayOf(2020, 2021, 2022, 2023))
        val stage = asg.generateStage(key, value)
        logger.info(stage)
        logger.info(stage.json)
        val expected = """
            { "@match": { "annotation.$key": { "@nin": [2020,2021,2022,2023] } } }
            """.trimIndent()
            .replace('@', '$')
        assertThatJson(stage.json).isEqualTo(expected)
    }

    @Test
    fun `special field matching - isNotIn with value other than list`() {
        val asg = AggregateStageGenerator(config)
        val key = "year"
        val value = mapOf(IS_NOT_IN to 1999)
        try {
            val stage = asg.generateStage(key, value)
            logger.info(stage)
            fail("expected call to fail")
        } catch (e: BadRequestException) {
            assertThat(e.message).isEqualTo(":isNotIn parameter must be a list")
        }
    }

    @Test
    fun `special field matching - isIn`() {
        val asg = AggregateStageGenerator(config)
        val key = "year"
        val value = mapOf(IS_IN to arrayOf(2020, 2021, 2022, 2023))
        val stage = asg.generateStage(key, value)
        logger.info(stage)
        logger.info(stage.json)
        val expected = """
            { "@match": { "annotation.$key": { "@in": [2020,2021,2022,2023] } } }
            """.trimIndent()
            .replace('@', '$')
        assertThatJson(stage.json).isEqualTo(expected)
    }

    @Test
    fun `special field matching - isIn with value other than list`() {
        val asg = AggregateStageGenerator(config)
        val key = "year"
        val value = mapOf(IS_IN to 2000)
        try {
            val stage = asg.generateStage(key, value)
            logger.info(stage)
            fail("expected call to fail")
        } catch (e: BadRequestException) {
            assertThat(e.message).isEqualTo(":isIn parameter must be a list")
        }
    }

    @Test
    fun `special field matching - isGreater`() {
        val asg = AggregateStageGenerator(config)
        val key = "year"
        val value = mapOf(IS_GREATER to 2000)
        val stage = asg.generateStage(key, value)
        logger.info(stage)
        logger.info(stage.json)
        val expected = """
            { "@match": { "annotation.$key": { "@gt": 2000 } } }
            """.trimIndent()
            .replace('@', '$')
        assertThatJson(stage.json).isEqualTo(expected)
    }

    @Test
    fun `special field matching - isGreaterOrEqual`() {
        val asg = AggregateStageGenerator(config)
        val key = "year"
        val value = mapOf(IS_GREATER_OR_EQUAL to 2000)
        val stage = asg.generateStage(key, value)
        logger.info(stage)
        logger.info(stage.json)
        val expected = """
            { "@match": { "annotation.$key": { "@gte": 2000 } } }
            """.trimIndent()
            .replace('@', '$')
        assertThatJson(stage.json).isEqualTo(expected)
    }

    @Test
    fun `special field matching - isLess`() {
        val asg = AggregateStageGenerator(config)
        val key = "year"
        val value = mapOf(IS_LESS to 2000)
        val stage = asg.generateStage(key, value)
        logger.info(stage)
        logger.info(stage.json)
        val expected = """
            { "@match": { "annotation.$key": { "@lt": 2000 } } }
            """.trimIndent()
            .replace('@', '$')
        assertThatJson(stage.json).isEqualTo(expected)
    }

    @Test
    fun `special field matching - isLessOrEqual`() {
        val asg = AggregateStageGenerator(config)
        val key = "year"
        val value = mapOf(IS_LESS_OR_EQUAL to 2000)
        val stage = asg.generateStage(key, value)
        logger.info(stage)
        logger.info(stage.json)
        val expected = """
            { "@match": { "annotation.$key": { "@lte": 2000 } } }
            """.trimIndent()
            .replace('@', '$')
        assertThatJson(stage.json).isEqualTo(expected)
    }

    @Test
    fun `special field matching - isEqualTo`() {
        val asg = AggregateStageGenerator(config)
        val key = "year"
        val value = mapOf(IS_EQUAL_TO to 2000)
        val stage = asg.generateStage(key, value)
        logger.info(stage)
        logger.info(stage.json)
        val expected = """
            { "@match": { "annotation.$key": 2000 } }
            """.trimIndent()
            .replace('@', '$')
        assertThatJson(stage.json).isEqualTo(expected)
    }

    @Test
    fun `special field matching - isNot`() {
        val asg = AggregateStageGenerator(config)
        val key = "year"
        val value = mapOf(IS_NOT to 2000)
        val stage = asg.generateStage(key, value)
        logger.info(stage)
        logger.info(stage.json)
        val expected = """
            { "@match": { "annotation.$key": { "@ne": 2000 } } }
            """.trimIndent()
            .replace('@', '$')
        assertThatJson(stage.json).isEqualTo(expected)
    }

    @Test
    fun `query functions - overlapping_with_range`() {
        val selector = "rangeSelectorType"
        every { config.rangeSelectorType } returns selector
        val asg = AggregateStageGenerator(config)
        val source = "http://example.com/some-id"
        val start = 200
        val end = 300
        val parameters = mapOf(
            "source" to source,
            "start" to start,
            "end" to end
        )
        val stage = asg.generateStage(OVERLAPPING_WITH_RANGE, parameters)
        logger.info(stage)
        val expected = """
            { 
                "@match": {
                    "annotation.target" : {
                        "@elemMatch": {
                            "@and": [
                                { "type": "Text"},
                                { "source": "$source"},
                                { "selector.type": "$selector"},
                                { "selector.start": { "@lte": ${end.toFloat()} } },
                                { "selector.end":   { "@gte": ${start.toFloat()} } }
                            ]
                        }
                    }
                }
            }""".trimIndent().replace('@', '$')
        assertThatJson(stage.json).isEqualTo(expected)
        logger.info(stage.json)

    }

    @Test
    fun `special field matching - or`() {
        val asg = AggregateStageGenerator(config)
        val key = OR
        val value = arrayOf(mapOf("body.type" to "Page"), mapOf("body.type" to "Line"))
        val stage = asg.generateStage(key, value)
        logger.info(stage)
        logger.info(stage.json)
        val expected = """
            {
                "@match": {
                    "@or": [
                        { "annotation.body.type": "Page"}, 
                        { "annotation.body.type": "Line"} 
                    ]
                }
            }
            """.trimIndent()
            .replace('@', '$')
        assertThatJson(stage.json).isEqualTo(expected)
    }
}