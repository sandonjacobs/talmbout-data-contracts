package io.confluent.devrel.datacontracts.shared

import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.Properties

abstract class BaseConsumer<Key, Value>(properties: Properties) {

    companion object {
        val logger = LoggerFactory.getLogger(BaseConsumer::class.java)
    }

    private val kafkaConsumer: KafkaConsumer<Key, Value> = KafkaConsumer(properties)

    fun start(topics: List<String>) {
        kafkaConsumer.subscribe(topics)
        while (true) {
            val records = kafkaConsumer.poll(Duration.ofSeconds(5))
            for (record in records) {
                logger.trace("Record from ${record.topic()}, ${record.partition()}, ${record.offset()}")
                consumeRecord(record.key(), record.value())
            }
        }
    }

    abstract fun consumeRecord(key: Key, value: Value)

    fun close() {
        kafkaConsumer.close()
    }
}