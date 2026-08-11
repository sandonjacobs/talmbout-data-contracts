import java.io.FileInputStream
import java.util.Properties

plugins {
    kotlin("jvm")
    id("com.google.protobuf") apply true
    alias(libs.plugins.schema.registry) apply true
    alias(libs.plugins.avro.gradle) apply true
}

// Temporarily disabled until Schema Registry credentials/config are wired back in.
// val schemaRegOutputDir = "${project.projectDir.absolutePath}/build/schema-registry-plugin"
//
// tasks.registerSchemasTask {
//     doFirst {
//         mkdir(schemaRegOutputDir)
//     }
// }
//
// schemaRegistry {
//     val srProperties = Properties()
//     // At the moment, this is a file with which we are LOCALLY aware.
//     // In an ACTUAL CI/CD workflow, this would be externalized, perhaps provided from a base build image or other parameter.
//     srProperties.load(FileInputStream(rootProject.file("shared/src/main/resources/confluent.properties")))
//
//     url = srProperties.getProperty("schema.registry.url")
//
//     val srCredTokens = srProperties.get("basic.auth.user.info").toString().split(":")
//     credentials {
//         username = srCredTokens[0]
//         password = srCredTokens[1]
//     }
//
//     outputDirectory = schemaRegOutputDir
//     pretty = true
//
//     val baseBuildDir = "${project.projectDir}/src/main"
//     val avroSchemaDir = "$baseBuildDir/avro"
//     val rulesetDir = "$baseBuildDir/rulesets"
//     val metadataDir = "$baseBuildDir/metadata"
//
//     register {
//         subject(inputSubject =  "membership-avro-value", type = "AVRO", file = "$avroSchemaDir/membership_v1.avsc")
//             .setMetadata("$metadataDir/membership_major_version_1.json")
//         subject(inputSubject =  "membership-avro-value", type = "AVRO", file = "$avroSchemaDir/membership_v2.avsc")
//             .addLocalReference("validity-period", "$avroSchemaDir/validity_period.avsc")
//             .setMetadata("$metadataDir/membership_major_version_2.json")
//             .setRuleSet("$rulesetDir/membership_migration_rules.json")
//     }
// }
