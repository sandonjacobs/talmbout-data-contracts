plugins {
    kotlin("jvm") apply true
}


dependencies {
    implementation(libs.slf4j.api)
    implementation(libs.slf4j.simple)
    implementation(libs.logback.core)

    implementation(libs.kotlin.faker)

    implementation(libs.clikt)
    implementation(libs.clikt.markdown)

    implementation(libs.grpc.stub)
    implementation(libs.grpc.protobuf)
    implementation(libs.google.protobuf.java)

    implementation(libs.apache.kafka.clients)
    implementation(libs.confluent.avro.serializer)
    implementation(libs.confluent.protobuf.serializer)
    implementation(libs.confluent.schema.rules)

    testImplementation("org.jetbrains.kotlin:kotlin-test")
}

kotlin {
    jvmToolchain(21)
}
