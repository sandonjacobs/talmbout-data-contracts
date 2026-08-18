package io.confluent.devrel.v2

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import io.confluent.devrel.v2.kafka.MembershipConsumer
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig
import org.apache.kafka.clients.consumer.ConsumerConfig
import java.util.*
import kotlin.concurrent.thread


fun main(args: Array<String>) = ConsumerCommand()
    .main(args)

class ConsumerCommand : CliktCommand() {

    val groupId: String by option("-g", "--group-id", help = "Group ID")
        .default("schema-v2")

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

    override fun run() {

        val consumerConfigOverrides = mapOf(
            ConsumerConfig.GROUP_ID_CONFIG to groupId,
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to "org.apache.kafka.common.serialization.StringDeserializer",
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to "io.confluent.kafka.serializers.KafkaAvroDeserializer",
            KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG to "true",
            AbstractKafkaSchemaSerDeConfig.LATEST_COMPATIBILITY_STRICT to true,
            AbstractKafkaSchemaSerDeConfig.USE_LATEST_VERSION to false,
            AbstractKafkaSchemaSerDeConfig.USE_LATEST_WITH_METADATA to "major_version=1"
        )

        val membershipConsumer =
            MembershipConsumer(loadConfig(configPath, consumerConfigOverrides))

        thread {
            membershipConsumer.start(listOf("membership-avro"))
        }
    }
}