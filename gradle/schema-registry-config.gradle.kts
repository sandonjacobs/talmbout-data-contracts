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

val srProperties = Properties()
FileInputStream(schemaRegistryConfigFile).use { srProperties.load(it) }

val srUrl = srProperties.getProperty("schema.registry.url")
    ?: throw GradleException("Missing 'schema.registry.url' in $schemaRegistryConfigFile")

// Local/no-auth registries won't have these keys; leave them out of the client config rather
// than requiring dummy values.
val srClientConfig = mutableMapOf<String, String>()
srProperties.getProperty("basic.auth.credentials.source")?.let { srClientConfig["basic.auth.credentials.source"] = it }
srProperties.getProperty("basic.auth.user.info")
    ?.takeIf { it.isNotBlank() }
    ?.let { srClientConfig["basic.auth.user.info"] = it }

project.extra["srUrl"] = srUrl
project.extra["srClientConfig"] = srClientConfig as Map<String, String>
