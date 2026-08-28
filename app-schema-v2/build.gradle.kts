import org.apache.avro.AvroRuntimeException
import org.apache.avro.Schema

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
    implementation(libs.jfiglet)

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
// validity_period.avsc). This task pre-resolves the downloaded schema files with a single
// shared Parser and writes out self-contained copies (referenced types inlined) that
// the plugin can then parse in isolation. Schemas are downloaded (rather than owned as
// committed source here) from the `schemas` module's Schema Registry registration.
val avroSourceDir = layout.buildDirectory.dir("downloaded-avro")
val resolvedAvroDir = layout.buildDirectory.dir("resolved-avro")

val resolveAvroSchemas by tasks.registering {
    group = "build"
    description = "Resolves cross-file Avro schema references into self-contained copies"

    dependsOn("downloadSchemasTask")

    inputs.dir(avroSourceDir)
    outputs.dir(resolvedAvroDir)

    doLast {
        val outputDir = resolvedAvroDir.get().asFile
        outputDir.deleteRecursively()
        outputDir.mkdirs()

        var remaining: List<File> = avroSourceDir.get().asFile.listFiles { file -> file.extension == "avsc" }?.toList()
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
                } catch (e: AvroRuntimeException) {
                    // Avro 1.12 eagerly resolves cross-type references at the end of each
                    // parse(File) call: parsing a file before its dependency is registered on
                    // the shared parser throws AvroTypeException ("Undefined schema: ..."),
                    // not SchemaParseException, even though it's the same self-ordering problem.
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

schemaRegistry {
    url = project.extra["srUrl"] as String

    @Suppress("UNCHECKED_CAST")
    clientConfig = project.extra["srClientConfig"] as Map<String, String>

    pretty = true

    // Pinned to version 2 explicitly (not "latest"). downloadReferences also writes the
    // referenced validity-period-value schema into the same output directory, so
    // resolveAvroSchemas above can inline it for codegen.
    download {
        subject(inputSubject = "membership-avro-value", outputPath = "${projectDir}/build/downloaded-avro", version = 2, downloadReferences = true)
    }
}

tasks.named("downloadSchemasTask") {
    doFirst {
        (project.extra["srConfigError"] as String?)?.let { throw GradleException(it) }
    }
}

