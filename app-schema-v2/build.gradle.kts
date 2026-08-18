import org.apache.avro.Schema
import org.apache.avro.SchemaParseException

plugins {
    kotlin("jvm") apply true
    application
    alias(libs.plugins.google.protobuf) apply true
    alias(libs.plugins.schema.registry) apply true
    alias(libs.plugins.avro.gradle) apply true
}

application {
    mainClass.set("io.confluent.devrel.v2.MainKt")
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

// The martinsjavacode avro-gradle-plugin parses each .avsc file with its own isolated
// Schema.Parser, so it can never resolve a type defined in a sibling file (e.g.
// membership_v2.avsc referencing io.confluent.devrel.ValidityPeriod from
// validity_period.avsc). This task pre-resolves the real source files with a single
// shared Parser and writes out self-contained copies (referenced types inlined) that
// the plugin can then parse in isolation without changing the on-disk source files,
// which stay separate so they can still be registered/reused independently in the
// Schema Registry.
val avroSourceDir = layout.projectDirectory.dir("src/main/avro")
val resolvedAvroDir = layout.buildDirectory.dir("resolved-avro")

val resolveAvroSchemas by tasks.registering {
    group = "build"
    description = "Resolves cross-file Avro schema references into self-contained copies"

    inputs.dir(avroSourceDir)
    outputs.dir(resolvedAvroDir)

    doLast {
        val outputDir = resolvedAvroDir.get().asFile
        outputDir.deleteRecursively()
        outputDir.mkdirs()

        var remaining = avroSourceDir.asFile.listFiles { file -> file.extension == "avsc" }?.toList()
            ?: emptyList()
        val resolved = linkedMapOf<File, Schema>()
        val parser = Schema.Parser()

        while (remaining.isNotEmpty()) {
            val stillPending = mutableListOf<File>()
            var progressed = false
            for (file in remaining) {
                try {
                    resolved[file] = parser.parse(file)
                    progressed = true
                } catch (e: SchemaParseException) {
                    stillPending.add(file)
                }
            }
            if (!progressed) {
                throw GradleException(
                    "Could not resolve Avro schema dependency for: " +
                        stillPending.joinToString(", ") { it.name },
                )
            }
            remaining = stillPending
        }

        resolved.forEach { (file, schema) ->
            File(outputDir, file.name).writeText(schema.toString(true))
        }
    }
}

avro {
    sourceDir.set(resolvedAvroDir.map { it.asFile.absolutePath })
}

tasks.named("validateAvroSchemas") {
    dependsOn(resolveAvroSchemas)
}

tasks.named("generateAvroClasses") {
    dependsOn(resolveAvroSchemas)
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
    dependsOn(resolveAvroSchemas)
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
        // Registered first so it exists as its own subject/version before membership-avro-value
        // below resolves a real reference to it (RegisterTaskAction registers subjects in the
        // order they're declared here).
        subject(inputSubject = "validity-period-value", type = "AVRO", file = "${projectDir}/src/main/avro/validity_period.avsc")

        subject(inputSubject = "membership-avro-value", type = "AVRO", file = "${projectDir}/src/main/avro/membership_v2.avsc")
            .addReference("io.confluent.devrel.ValidityPeriod", "validity-period-value", -1)
            .setMetadata("${projectDir}/src/main/rules/membership_major_version_2.json")
            .setRuleSet("${projectDir}/src/main/rules/membership_migration.json")

    }
}

