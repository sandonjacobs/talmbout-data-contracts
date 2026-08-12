package io.confluent.devrel.dc.v1.kafka

import io.confluent.devrel.Membership
import io.confluent.devrel.datacontracts.shared.BaseConsumer
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig
import org.apache.kafka.clients.consumer.ConsumerConfig
import java.util.Properties

class MembershipConsumer(properties: Properties): BaseConsumer<String, Membership>(properties) {

    override fun consumeRecord(
        key: String,
        value: Membership
    ) {
        logger.info("Received Membership ${key}, $value")
    }
}