# AGENTS.md

This file provides guidance to agents when working with code in this repository.

## Project Overview

This is a demonstration project showcasing **Data Contracts in Confluent Cloud** with a focus on schema evolution using migration rules. The project illustrates how producers and consumers can independently evolve schemas while maintaining compatibility through Confluent Schema Registry's migration rules feature.

### Core Technologies

- **Language**: Kotlin (JVM)
- **Build System**: Gradle 8.5+ with Kotlin DSL
- **Kafka Platform**: Apache Kafka with Confluent Cloud
- **Schema Format**: Apache Avro
- **Infrastructure**: Terraform (Confluent Cloud Provider)
- **JDK Version**: 21

### Architecture

The project demonstrates a multi-version schema evolution pattern:

1. **Infrastructure Layer** (`cc-terraform/`): Terraform configurations provision Confluent Cloud resources (environment, Kafka cluster, Schema Registry with Stream Governance)

2. **Schema Management** (`schemas/`): Owned by the data architect/engineer persona. Contains the Avro schema definitions, metadata, and migration rulesets for both major versions, and registers them with Schema Registry via two independent custom Gradle tasks (`registerSchemasV1`, `registerSchemasV2`) rather than one combined task — each version is registered (and demoed) on its own.

3. **Shared Components** (`shared/`): Base classes for Kafka producers and consumers with common configuration handling

4. **Application Versions** (developer persona; no longer own any `.avsc`/rules source — each downloads the specific registered version it builds against via the imflog plugin's `downloadSchemasTask`):
   - `app-schema-v1`: Producer and consumer using schema version 1 (with `start_date` and `end_date` fields)
   - `app-schema-v2`: Producer and consumer using schema version 2 (with `validity_period` nested structure)

The key architectural pattern is that `app-schema-v1` produces events using v1 schema, while `app-schema-v2` consumes the same events using v2 schema. Migration rules in Schema Registry handle the transformation automatically. Because both app modules' builds depend on `downloadSchemasTask`, `:schemas:registerSchemasV1`/`:schemas:registerSchemasV2` must be run at least once before `app-schema-v1`/`app-schema-v2` can build.

## Building and Running

### Prerequisites

- JDK 21
- Gradle 8.5+ (wrapper included)
- Confluent Cloud account
- Confluent CLI installed and configured
- Terraform installed
- jq (for setup script)

### Environment Setup

1. **Configure Confluent Cloud credentials**:
   ```bash
   export CONFLUENT_CLOUD_API_KEY=<your-api-key>
   export CONFLUENT_CLOUD_API_SECRET=<your-api-secret>
   ```

2. **Set Terraform organization ID**:
   ```bash
   export TF_VAR_org_id=$(confluent organization list -o json | jq -c -r '.[] | select(.is_current)' | jq '.id')
   ```

3. **Run setup script** (provisions Confluent Cloud and builds all modules):
   ```bash
   chmod +x setup.sh
   ./setup.sh
   ```

   This script:
   - Initializes Terraform and provisions Confluent Cloud resources
   - Downloads Gradle wrapper JARs
   - Registers schemas with Schema Registry
   - Generates Java classes from Avro schemas
   - Creates `shared/src/main/resources/confluent.properties` with connection details

### Build Commands

```bash
# Build all modules
./gradlew build

# Build specific module
./gradlew :app-schema-v1:build
./gradlew :shared:build

# Generate Avro classes (downloads the module's schema version first)
./gradlew :app-schema-v1:generateAvroClasses

# Register schemas with Schema Registry (run before building app-schema-v1/v2)
./gradlew :schemas:registerSchemasV1
./gradlew :schemas:registerSchemasV2
```

### Running Applications

**Schema v1 Application** - Run consumer and producer in separate terminals:

Consumer (in one terminal):
```bash
./gradlew :app-schema-v1:run --args="consumer"

# With custom config and group ID
./gradlew :app-schema-v1:run --args="consumer -p /local.properties -g my-group"
```

Producer (in another terminal):
```bash
# Default: produces 1 event/second for 10 seconds
./gradlew :app-schema-v1:run --args="producer"

# Custom intervals
./gradlew :app-schema-v1:run --args="producer -i 2 -d 60"  # 2 sec interval, 60 sec duration

# With custom config file
./gradlew :app-schema-v1:run --args="producer -p /local.properties"
```

**Schema v2 Application** (Consumer only):
```bash
./gradlew :app-schema-v2:run
```

### Schema Registry Operations

The project uses the `kafka-schema-registry-gradle-plugin` (imflog) for schema management. Registration lives
in `schemas` (architect/engineer side); download lives in each `app-schema-v*` module (developer side):

```bash
# Register schemas (with Confluent Cloud) - one task per major version, run independently
./gradlew :schemas:registerSchemasV1
./gradlew :schemas:registerSchemasV2

# Register schemas (with local Schema Registry)
./gradlew :schemas:registerSchemasV1 -PschemaRegistryConfig=local.properties

# Download the schema a given app module builds against (pinned to that module's version)
./gradlew :app-schema-v1:downloadSchemasTask
./gradlew :app-schema-v2:downloadSchemasTask
```

### Teardown

```bash
cd cc-terraform
terraform destroy -auto-approve
```

## Development Conventions

### Project Structure

- **Multi-module Gradle project** with shared dependencies managed via `gradle/libs.versions.toml`
- **Kotlin DSL** for all Gradle build files
- **Source generation**: Avro schemas generate Java classes at build time into `build/generated/java`
- **Configuration management**: Properties files in `shared/src/main/resources/` (not committed to git)

### Kafka Client Patterns

1. **Base Classes**: All producers extend `BaseProducer<Key, Value>`, all consumers extend `BaseConsumer<Key, Value>`
2. **Configuration**: Properties loaded from classpath resources, with support for overrides
3. **Serialization**: Uses Confluent's Avro serializers with Schema Registry integration
4. **Error Handling**: Logging via SLF4J with callback-based error handling in producers

### Schema Evolution Strategy

- **Major version changes**: Handled via migration rules in Schema Registry
- **Consumer configuration**: Uses `use.latest.version=true` and `use.latest.with.metadata` for automatic schema resolution
- **Backward compatibility**: Migration rules transform v1 events to v2 format transparently

### Command-Line Interface

Applications use **Clikt** library for CLI argument parsing with these common options:
- `-p, --config-path`: Path to properties file (default: `/confluent.properties`)
- `-c, --client-id`: Kafka client ID
- `-g, --group-id`: Consumer group ID
- `-i, --interval`: Producer interval in seconds
- `-d, --duration`: Producer duration in seconds

### Testing

The project currently focuses on demonstration rather than comprehensive testing. When adding tests:
- Use JUnit 5 (Jupiter)
- Place tests in standard `src/test/kotlin` directories
- Use AssertJ for assertions (already in dependencies)

### Logging

- **Framework**: SLF4J API with Logback implementation
- **Configuration**: `logback.xml` in each module's resources
- **Pattern**: Structured logging with context (user IDs, topics, partitions)
- **Levels**: DEBUG for detailed message info, INFO for key events, ERROR for failures

### Security Considerations

- **Credentials**: Never commit `confluent.properties` or similar config files (use `.gitignore`)
- **Secrets Management**: For production, use secrets managers (Vault, cloud provider solutions) instead of properties files
- **TLS**: All Confluent Cloud connections use TLS by default
- **Authentication**: Uses API keys for Schema Registry and Kafka cluster access

### Local Development Alternative

To run against local Kafka/Schema Registry instead of Confluent Cloud:
1. Copy `shared/src/main/resources/.local.properties.template` to `local.properties`
2. Configure `bootstrap.servers` and `schema.registry.url`
3. Pass `-PschemaRegistryConfig=local.properties` to Gradle tasks
4. Pass `-p /local.properties` to application run commands

## Key Files and Their Purpose

- `build.gradle.kts`: Root build configuration with plugin versions and repository setup
- `settings.gradle.kts`: Multi-module project structure definition
- `gradle/libs.versions.toml`: Centralized dependency version management
- `setup.sh`: Automated setup script for Confluent Cloud and Gradle builds
- `cc-terraform/*.tf`: Infrastructure as Code for Confluent Cloud resources
- `shared/src/main/kotlin/.../Base*.kt`: Reusable Kafka client abstractions
- `schemas/src/main/avro/*.avsc`: Avro schema definitions (all versions)
- `schemas/src/main/rules/*.json`: Schema Registry metadata and migration rules (all versions)
- `app-schema-v*/build/downloaded-avro/*.avsc`: schemas downloaded at build time by `downloadSchemasTask`; not committed source

## Common Tasks

### Adding a New Schema Version

1. Add the new Avro schema (and any newly-referenced schemas) under `schemas/src/main/avro/`
2. Add its metadata/migration-rule JSON under `schemas/src/main/rules/`
3. Add a new custom task in `schemas/build.gradle.kts` (e.g. `registerSchemasV3`), following the
   `registerSchemasV1`/`registerSchemasV2` pattern
4. Create the new app module (e.g., `app-schema-v3`), configured to `download` that version's
   subject via the `schemaRegistry { download { ... } }` block, pinned to the new version number
5. Implement producer/consumer using shared base classes
6. Update `settings.gradle.kts` to include the new module

### Modifying Existing Schema

1. Update the `.avsc` file in `schemas/src/main/avro/`
2. Update or create migration rules in `schemas/src/main/rules/`
3. Re-register the schema: `./gradlew :schemas:registerSchemasV1` (or `V2`)
4. Re-download and regenerate classes in the consuming app module: `./gradlew :module:generateAvroClasses`
5. Update application code as needed

### Debugging Schema Issues

1. Check Schema Registry: `confluent schema-registry schema list`
2. View schema details: `confluent schema-registry schema describe --subject <subject>`
3. Enable DEBUG logging in `logback.xml`
4. Use `--info` or `--debug` flags with Gradle commands

## Additional Notes

- The project is based on work by Gilles Philippart (see README for original repository)
- Schema Registry uses "ADVANCED" Stream Governance package for migration rules support
- The `membership-avro` topic is the central event stream for the demo
- Consumer groups are version-specific to demonstrate independent evolution
