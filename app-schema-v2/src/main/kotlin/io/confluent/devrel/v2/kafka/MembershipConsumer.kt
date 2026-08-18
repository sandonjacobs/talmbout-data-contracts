package io.confluent.devrel.v2.kafka

import io.confluent.devrel.Membership
import io.confluent.devrel.datacontracts.shared.BaseConsumer
import java.util.Properties

class MembershipConsumer(properties: Properties): BaseConsumer<String, Membership>(properties) {

    override fun consumeRecord(
        key: String,
        value: Membership
    ) {
        logger.info("v2 -Received Membership ${key}, $value")
    }
}