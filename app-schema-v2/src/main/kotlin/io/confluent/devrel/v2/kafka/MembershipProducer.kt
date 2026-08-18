package io.confluent.devrel.v2.kafka

import io.confluent.devrel.Membership
import io.confluent.devrel.datacontracts.shared.BaseProducer
import java.util.Properties

class MembershipProducer(properties: Properties): BaseProducer<String, Membership>(properties)
