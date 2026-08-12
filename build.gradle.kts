buildscript {
    repositories {
        mavenCentral()
        maven("https://packages.confluent.io/maven/")
        gradlePluginPortal()
    }
}

plugins {
    kotlin("jvm") version "2.4.10" apply false
    alias(libs.plugins.schema.registry) apply false
    alias(libs.plugins.google.protobuf) apply false
    alias(libs.plugins.avro.gradle) apply false
}

allprojects {
    group = "io.confluent.devrel"

    buildscript {
        repositories {
            mavenCentral()
            maven("https://packages.confluent.io/maven/")
            maven("https://jitpack.io")
            gradlePluginPortal()
        }
    }

    repositories {
        mavenLocal()
        mavenCentral()
        maven("https://packages.confluent.io/maven/")
        maven("https://jitpack.io")
        gradlePluginPortal()
    }
}