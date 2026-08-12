plugins {
    kotlin("jvm") apply true
    alias(libs.plugins.google.protobuf) apply true
    alias(libs.plugins.schema.registry) apply true
    alias(libs.plugins.avro.gradle) apply true
}

sourceSets {
    main {
        kotlin.srcDirs("src/main/kotlin", "build/generated/java")
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
//    implementation(libs.kotlin.faker.books)
//    implementation(libs.kotlin.faker.tech)

    implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.6")
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

// Temporarily disabled until Schema Registry credentials/config are wired back in.
// val schemaRegOutputDir = "${project.projectDir.absolutePath}/build/schema-registry-plugin"
//
// tasks.downloadSchemasTask {
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
//     outputDirectory = "${System.getProperty("user.home")}/tmp/schema-registry-plugin"
//     pretty = true
//
//     download {
//         // download the membership avro schema, version 1
//         subject("membership-avro-value", "${projectDir}/src/main/avro", 1)
//     }
// }

//tasks.clean {
//    doFirst {
//        delete(fileTree("${projectDir}/src/main/avro/").include("**/*.avsc"))
//    }
//}

//tasks.register("generateCode") {
//    group = "source generation"
//    description = "wrapper task for all source generation"
//    dependsOn("downloadSchemasTask", "generateAvroJava", "generateProto")
//}
