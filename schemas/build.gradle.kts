import com.github.imflog.schema.registry.tasks.register.RegisterSchemasTask
import com.github.imflog.schema.registry.tasks.register.RegisterSubjectExtension

plugins {
    alias(libs.plugins.schema.registry) apply true
}

apply(from = "$rootDir/gradle/schema-registry-config.gradle.kts")

// Data architect/engineer domain: register each schema version, its metadata, and its migration
// ruleset with Schema Registry. Two independent tasks (not one "register everything" task) so
// each major version can be demoed/registered on its own. The imflog plugin's schemaRegistry{}
// DSL only wires up a single fixed-name registerSchemasTask, so these instantiate its underlying
// RegisterSubjectExtension/RegisterSchemasTask classes directly instead.

val srUrl = project.extra["srUrl"] as String
val srConfigError = project.extra["srConfigError"] as String?

@Suppress("UNCHECKED_CAST")
val srClientConfig = project.extra["srClientConfig"] as Map<String, String>

val registerV1Subjects = RegisterSubjectExtension(project.objects).apply {
    subject(inputSubject = "membership-avro-value", file = "$projectDir/src/main/avro/membership_v1.avsc", type = "AVRO")
        .setMetadata("$projectDir/src/main/rules/membership_major_version_1.json")
}

tasks.register<RegisterSchemasTask>("registerSchemasV1") {
    group = "registry"
    description = "Register the v1 Membership schema (major_version=1) with Schema Registry"
    url.set(srUrl)
    clientConfig.set(srClientConfig)
    subjects.set(registerV1Subjects.subjects)
    outputDirectory.set("${projectDir}/schema-registry-plugin/v1")
    rootDir.set(projectDir)
    failFast.set(true)
    doFirst {
        srConfigError?.let { throw GradleException(it) }
        mkdir("${projectDir}/schema-registry-plugin/v1")
    }
}

val registerV2Subjects = RegisterSubjectExtension(project.objects).apply {
    // ValidityPeriod and Address are only used by v2, so they're defined as nested types
    // directly in membership_v2.avsc rather than as separate referenced subjects.
    subject(inputSubject = "membership-avro-value", file = "$projectDir/src/main/avro/membership_v2.avsc", type = "AVRO")
        .setMetadata("$projectDir/src/main/rules/membership_major_version_2.json")
        .setRuleSet("$projectDir/src/main/rules/membership_migration.json")
}

tasks.register<RegisterSchemasTask>("registerSchemasV2") {
    group = "registry"
    description = "Register the v2 Membership schema (major_version=2, with nested ValidityPeriod/Address) with the migration ruleset"
    url.set(srUrl)
    clientConfig.set(srClientConfig)
    subjects.set(registerV2Subjects.subjects)
    outputDirectory.set("${projectDir}/schema-registry-plugin/v2")
    rootDir.set(projectDir)
    failFast.set(true)
    doFirst {
        srConfigError?.let { throw GradleException(it) }
        mkdir("${projectDir}/schema-registry-plugin/v2")
    }
}
