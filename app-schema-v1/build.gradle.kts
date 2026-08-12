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
    sourceDir.set("src/main/avro")
}

tasks.named("compileKotlin") {
    dependsOn("generateAvroClasses")
}

tasks.named("compileJava") {
    dependsOn("generateAvroClasses")
}

apply(from = "$rootDir/gradle/schema-registry-config.gradle.kts")

val schemaRegOutputDir = "${project.projectDir.absolutePath}/build/schema-registry-plugin"

tasks.registerSchemasTask {
    doFirst {
        mkdir(schemaRegOutputDir)
    }
}

schemaRegistry {
    url = project.extra["srUrl"] as String

    @Suppress("UNCHECKED_CAST")
    clientConfig = project.extra["srClientConfig"] as Map<String, String>

    outputDirectory = schemaRegOutputDir
    pretty = true

    register {
        subject(inputSubject = "membership-avro-value", type = "AVRO", file = "${projectDir}/src/main/avro/membership_v1.avsc")
            //.setMetadata("${projectDir}/src/main/rules/membership_major_version_1.json")
    }
}

