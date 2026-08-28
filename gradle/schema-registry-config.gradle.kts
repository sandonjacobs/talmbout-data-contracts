import java.io.FileInputStream
import java.util.Properties

// Resolves Schema Registry connection info for the register/download Gradle tasks from a
// properties file under shared/src/main/resources/. Defaults to confluent.properties (Confluent
// Cloud); pass -PschemaRegistryConfig=local.properties (or any other file in that directory) to
// point register/download tasks at a different Kafka/Schema Registry deployment, e.g. a local
// docker/podman stack. Exposes srUrl (String) and srClientConfig (Map<String, String>) via
// project.extra for the applying build script to wire into its schemaRegistry {} block. The
// kafka-schema-registry-gradle-plugin passes clientConfig straight through to
// CachedSchemaRegistryClient, which itself understands the standard
// basic.auth.credentials.source / basic.auth.user.info keys (no dedicated credentials {} DSL as
// of plugin 2.5.0), so we just forward whichever of those keys are present.

val schemaRegistryConfigFileName = (project.findProperty("schemaRegistryConfig") as String?) ?: "confluent.properties"
val schemaRegistryConfigFile = rootProject.file("shared/src/main/resources/$schemaRegistryConfigFileName")

// Read leniently at configuration time: tasks that never touch Schema Registry (clean, tasks,
// help, ...) shouldn't fail just because this file is absent. Applying build scripts are
// responsible for wiring a doFirst check (using srConfigError below) onto whichever of their own
// tasks actually needs srUrl/srClientConfig, so the clear error only surfaces when one of those
// tasks runs.
var srUrl: String? = null

// Local/no-auth registries won't have these keys; leave them out of the client config rather
// than requiring dummy values.
val srClientConfig = mutableMapOf<String, String>()

if (schemaRegistryConfigFile.exists()) {
    val srProperties = Properties()
    FileInputStream(schemaRegistryConfigFile).use { srProperties.load(it) }

    srUrl = srProperties.getProperty("schema.registry.url")

    srProperties.getProperty("basic.auth.credentials.source")?.let { srClientConfig["basic.auth.credentials.source"] = it }
    srProperties.getProperty("basic.auth.user.info")
        ?.takeIf { it.isNotBlank() }
        ?.let { srClientConfig["basic.auth.user.info"] = it }
}

project.extra["srUrl"] = srUrl ?: ""
project.extra["srClientConfig"] = srClientConfig as Map<String, String>
project.extra["srConfigError"] = if (srUrl == null) {
    "Missing '$schemaRegistryConfigFile' (or its 'schema.registry.url' property); required to reach Schema Registry for this task."
} else {
    null
}
