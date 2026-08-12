package io.confluent.devrel.dc.v1

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import io.confluent.devrel.Membership
import io.confluent.devrel.dc.v1.kafka.MembershipConsumer
import io.confluent.devrel.dc.v1.kafka.MembershipProducer
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import java.time.LocalDate
import java.util.*
import kotlin.concurrent.thread
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.DurationUnit
import kotlin.time.toDuration

fun main() {


}

abstract class RootCommand : CliktCommand() {
    val configPath: String by option("-p", "--config-path", help = "Path to config file")
        .default("/confluent.properties")

    val clientId: String by option("-c", "--client-id", help = "Client ID")
        .default("schema-v1")

    fun loadConfig(
        path: String = "/confluent.properties",
        overrides: Map<String, Any> = emptyMap()
    ): Properties {
        val props = Properties()
        props.load(this::class.java.classLoader?.getResourceAsStream(path))
        props.putAll(overrides)
        return props
    }
}

class ConsumerCommand : RootCommand() {

    val groupId: String by option("-g", "--group-id", help = "Group ID")
        .default("schema-v1")

    override fun run() {

        val consumerConfigOverrides = mapOf(
            ConsumerConfig.GROUP_ID_CONFIG to groupId,
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to "org.apache.kafka.common.serialization.StringDeserializer",
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to "io.confluent.kafka.serializers.KafkaAvroDeserializer",
            "specific.avro.reader" to "true"
        )

        val membershipConsumer =
            MembershipConsumer(loadConfig(overrides = consumerConfigOverrides))

        thread {
            membershipConsumer.start(listOf("membership-avro"))
        }
    }
}

class ProducerCommand : RootCommand() {

    val interval: Int by option("-i", "--interval", help = "Interval in seconds")
        .int()
        .default(1)
    val duration: Int by option("-d", "--duration", help = "Duration in seconds")
        .int()
        .default(10)

    override fun run() {

        val howOften = interval.toDuration(DurationUnit.SECONDS)
        val howLong = duration.toDuration(DurationUnit.SECONDS)

        val producerConfigOverrides = mapOf(
            ProducerConfig.CLIENT_ID_CONFIG to clientId,
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to "org.apache.kafka.common.serialization.StringSerializer",
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to "io.confluent.kafka.serializers.KafkaAvroSerializer"
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
                    while (Clock.System.now() < until) {
                        val userId = UUID.randomUUID().toString()
                        val membership = Membership.newBuilder()
                            .setUserId(userId)
                            .setStartDate(LocalDate.now().minusDays(kotlin.random.Random.nextLong(100, 1000)))
                            .setEndDate(LocalDate.now().plusWeeks(Random.nextLong(1, 52)))
                            .build()
                        producer.send("membership-avro", userId, membership)
                        delay(howOften.inWholeSeconds)
                    }
                }
            }
            producer.close()
        }
    }
}