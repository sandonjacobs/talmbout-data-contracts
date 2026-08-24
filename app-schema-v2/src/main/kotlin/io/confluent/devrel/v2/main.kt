package io.confluent.devrel.v2

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import io.confluent.devrel.Membership
import io.confluent.devrel.ValidityPeriod
import io.confluent.devrel.v2.kafka.MembershipConsumer
import io.confluent.devrel.v2.kafka.MembershipProducer
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.*
import kotlin.concurrent.thread
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.DurationUnit
import kotlin.time.toDuration


val logger: Logger? = LoggerFactory.getLogger("io.confluent.devrel.v2.main")

fun main(args: Array<String>) = ApplicationCommand()
    .subcommands(ConsumerCommand(), ProducerCommand())
    .main(args)

class ApplicationCommand : CliktCommand(name = "app-schema-v2") {
    override fun run() = Unit
}

abstract class RootCommand : CliktCommand() {
    val configPath: String by option("-p", "--config-path", help = "Path to config file")
        .default("/confluent.properties")

    val clientId: String by option("-c", "--client-id", help = "Client ID")
        .default("schema-v2")

    fun loadConfig(
        path: String = "/confluent.properties",
        overrides: Map<String, Any> = emptyMap()
    ): Properties {
        val props = Properties()
        props.load(this::class.java.getResourceAsStream(path))
        props.putAll(overrides)
        return props
    }
}

class ConsumerCommand : RootCommand() {

    val groupId: String by option("-g", "--group-id", help = "Group ID")
        .default("schema-v2")

    override fun run() {

        val consumerConfigOverrides = mapOf(
            ConsumerConfig.GROUP_ID_CONFIG to groupId,
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to "org.apache.kafka.common.serialization.StringDeserializer",
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to "io.confluent.kafka.serializers.KafkaAvroDeserializer",
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
            KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG to "true",
            AbstractKafkaSchemaSerDeConfig.LATEST_COMPATIBILITY_STRICT to true,
            AbstractKafkaSchemaSerDeConfig.USE_LATEST_VERSION to false,
            AbstractKafkaSchemaSerDeConfig.USE_LATEST_WITH_METADATA to "major_version=2"
        )

        val membershipConsumer =
            MembershipConsumer(loadConfig(configPath, consumerConfigOverrides))

        thread {
            membershipConsumer.start(listOf("membership-avro"))
        }
    }
}

class ProducerCommand : RootCommand() {

    val interval: Int by option("-i", "--interval", help = "Interval in milliseconds")
        .int()
        .default(100)
    val duration: Int by option("-d", "--duration", help = "Duration in seconds")
        .int()
        .default(30)

    override fun run() {

        val howOften = interval.toDuration(DurationUnit.MILLISECONDS)
        val howLong = duration.toDuration(DurationUnit.SECONDS)

        val producerConfigOverrides = mapOf(
            ProducerConfig.CLIENT_ID_CONFIG to clientId,
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to "org.apache.kafka.common.serialization.StringSerializer",
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to "io.confluent.kafka.serializers.KafkaAvroSerializer",
            AbstractKafkaSchemaSerDeConfig.USE_LATEST_VERSION to true,
            AbstractKafkaSchemaSerDeConfig.LATEST_COMPATIBILITY_STRICT to true,
            AbstractKafkaSchemaSerDeConfig.USE_LATEST_WITH_METADATA to "major_version=2"
        )
        val producer = MembershipProducer(
            loadConfig(
                configPath,
                producerConfigOverrides
            )
        )

        runBlocking {
            coroutineScope {
                launch {
                    val until = Clock.System.now().plus(howLong)
                    logger?.info("---> Sending messages until {}", until)
                    while (Clock.System.now() < until) {
                        val userId = UUID.randomUUID().toString()
                        val membership = Membership.newBuilder()
                            .setUserId(userId)
                            .setValidityPeriod(
                                ValidityPeriod.newBuilder()
                                    .setFrom(LocalDate.now().minusDays(kotlin.random.Random.nextLong(100, 1000)))
                                    .setTo(LocalDate.now().plusWeeks(Random.nextLong(1, 52)))
                                    .build()
                            )
                            .build()
                        producer.send("membership-avro", userId, membership)
                        logger?.debug("---> Sent message with userId {}, sleeping for {}", userId, howOften)
                        delay(howOften)
                    }
                }
            }
            producer.close()
        }
    }
}
