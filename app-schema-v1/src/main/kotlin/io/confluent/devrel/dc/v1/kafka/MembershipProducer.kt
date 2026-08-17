package io.confluent.devrel.dc.v1.kafka

import io.confluent.devrel.Membership
import io.confluent.devrel.datacontracts.shared.BaseProducer
import java.util.*

class MembershipProducer(properties: Properties): BaseProducer<String, Membership>(properties)
