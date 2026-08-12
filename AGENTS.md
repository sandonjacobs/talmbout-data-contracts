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

2. **Schema Management** (`schemas/` - referenced but not present in current structure): Contains Avro schema definitions and migration rulesets

3. **Shared Components** (`shared/`): Base classes for Kafka producers and consumers with common configuration handling

4. **Application Versions**:
   - `app-schema-v1`: Producer and consumer using schema version 1 (with `start_date` and `end_date` fields)
   - `app-schema-v2`: Consumer using schema version 2 (with `validity_period` nested structure)

The key architectural pattern is that `app-schema-v1` produces events using v1 schema, while `app-schema-v2` consumes the same events using v2 schema. Migration rules in Schema Registry handle the transformation automatically.

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

# Generate Avro classes
./gradlew :app-schema-v1:generateAvroClasses

# Register schemas with Schema Registry
./gradlew :app-schema-v1:registerSchemasTask
```

### Running Applications

**Schema v1 Application** (Producer + Consumer):
```bash
# Default: produces 1 event/second for 100 seconds
./gradlew :app-schema-v1:run

# Custom intervals
./gradlew :app-schema-v1:run --args="-i 2 -d 60"  # 2 sec interval, 60 sec duration

# With custom config file
./gradlew :app-schema-v1:run --args="-p /local.properties"
```

**Schema v2 Application** (Consumer only):
```bash
./gradlew :app-schema-v2:run
```

### Schema Registry Operations

The project uses the `kafka-schema-registry-gradle-plugin` for schema management:

```bash
# Register schemas (with Confluent Cloud)
./gradlew :app-schema-v1:registerSchemasTask

# Register schemas (with local Schema Registry)
./gradlew :app-schema-v1:registerSchemasTask -PschemaRegistryConfig=local.properties

# Download schemas
./gradlew :app-schema-v1:downloadSchemasTask
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
1. Copy `shared/src/main/resources/local.properties.orig` to `local.properties`
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
- `app-schema-v*/src/main/avro/*.avsc`: Avro schema definitions
- `app-schema-v*/src/main/rules/*.json`: Schema Registry migration rules

## Common Tasks

### Adding a New Schema Version

1. Create new module (e.g., `app-schema-v3`)
2. Add Avro schema in `src/main/avro/`
3. Configure `build.gradle.kts` with schema registry plugin
4. Define migration rules in `src/main/rules/`
5. Implement producer/consumer using shared base classes
6. Update `settings.gradle.kts` to include new module

### Modifying Existing Schema

1. Update `.avsc` file in `src/main/avro/`
2. Update or create migration rules in `src/main/rules/`
3. Re-register schema: `./gradlew :module:registerSchemasTask`
4. Regenerate classes: `./gradlew :module:generateAvroClasses`
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
