plugins {
    kotlin("jvm") apply true
    application
    alias(libs.plugins.google.protobuf) apply true
    alias(libs.plugins.schema.registry) apply true
    alias(libs.plugins.avro.gradle) apply true
}

application {
    mainClass.set("io.confluent.devrel.dc.v1.MainKt")
}

sourceSets {
    main {
        kotlin.srcDirs("src/main/kotlin", "build/generated/java")
        java.srcDirs("build/generated/java")
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.kotlinx.coroutines.core)

    implementation(libs.clikt)
    implementation(libs.clikt.markdown)

    implementation(libs.apache.kafka.clients)
    implementation(libs.confluent.avro.serializer)

    implementation(libs.kotlin.faker)
    implementation(libs.kotlinx.datetime)
}

kotlin {
    jvmToolchain(21)
}

avro {
    sourceDir.set("build/downloaded-avro")
}

tasks.named("validateAvroSchemas") {
    dependsOn("downloadSchemasTask")
}

tasks.named("generateAvroClasses") {
    dependsOn("downloadSchemasTask")
}

tasks.named("compileKotlin") {
    dependsOn("generateAvroClasses")
}

tasks.named("compileJava") {
    dependsOn("generateAvroClasses")
}

apply(from = "$rootDir/gradle/schema-registry-config.gradle.kts")

schemaRegistry {
    url = project.extra["srUrl"] as String

    @Suppress("UNCHECKED_CAST")
    clientConfig = project.extra["srClientConfig"] as Map<String, String>

    pretty = true

    // Pinned to version 1 explicitly (not "latest") - once schemas:registerSchemasV2 has run,
    // "latest" on this subject resolves to the v2 schema, which would be wrong for this module.
    download {
        subject(inputSubject = "membership-avro-value", outputPath = "${projectDir}/build/downloaded-avro", version = 1)
    }
}

