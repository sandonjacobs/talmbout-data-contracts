# Makefile Documentation

This project uses a modular Makefile system to orchestrate demo tasks against Confluent Cloud.

## Quick Start

### 1. Initialize the Project

```bash
make init
```

This shows the prerequisites and next steps for setting up Confluent Cloud.

### 2. Setup Your Environment

```bash
# Set your credentials
export CONFLUENT_CLOUD_API_KEY=<your-key>
export CONFLUENT_CLOUD_API_SECRET=<your-secret>
export TF_VAR_org_id=$(confluent organization list -o json | jq -c -r '.[] | select(.is_current)' | jq '.id')

# Setup infrastructure
make setup-cloud
```

### 3. Build the Project

```bash
make build-v1
```

### 4. Register Schemas

```bash
make register-schemas-v1
```

### 5. Run the Demo

**Schema v1 Demo** (open two terminals):

Terminal 1 - Consumer:
```bash
make demo-consumer-v1
```

Terminal 2 - Producer:
```bash
make demo-producer-v1
```

**Schema Evolution Demo** (v1 → v2, open three terminals):

Terminal 1 - v1 Consumer:
```bash
make demo-consumer-v1
```

Terminal 2 - v2 Consumer (shows migration):
```bash
make demo-consumer-v2
```

Terminal 3 - v1 Producer:
```bash
make demo-producer-v1
```

## Makefile Structure

The Makefile system is organized into modular files:

```
Makefile                    # Main entry point with help
make/
├── init.mk                 # Initialization
├── confluent-cloud.mk      # Confluent Cloud setup and teardown
├── gradle.mk               # Gradle build tasks
└── demo.mk                 # Demo orchestration
```

## Available Commands

Run `make help` to see all available commands. Key commands are organized by category:

### Initialization

- `make init` - Show prerequisites and next steps for Confluent Cloud setup
- `make clean-env` - Remove .env configuration file

### Confluent Cloud

- `make setup-cloud` - Setup Confluent Cloud environment
- `make check-cloud-credentials` - Verify credentials are set
- `make terraform-init` - Initialize Terraform
- `make terraform-plan` - Create Terraform plan
- `make terraform-apply` - Apply Terraform configuration
- `make export-cloud-config` - Export connection properties
- `make cloud-status` - Show resource status
- `make cloud-output` - Show Terraform outputs
- `make teardown-cloud` - Destroy all cloud resources

### Gradle Build

- `make build-v1` - Build app-schema-v1 module
- `make build-v2` - Build app-schema-v2 module
- `make clean-build` - Clean build artifacts
- `make test` - Run tests
- `make register-schemas-v1` - Register v1 schemas with Schema Registry
- `make register-schemas-v2` - Register v2 schemas with Schema Registry
- `make download-schemas` - Download schemas from Schema Registry

### Demo

- `make demo-v1` - Show instructions for v1 demo
- `make demo-v2` - Show instructions for v2 evolution demo
- `make demo-producer-v1` - Run v1 producer
- `make demo-consumer-v1` - Run v1 consumer
- `make demo-consumer-v2` - Run v2 consumer (with migration)
- `make demo-full` - Show all demo parameters
- `make demo-reset` - Reset consumer group offsets
- `make demo-status` - Show topics and consumer groups

## Configuration

### Confluent Cloud Credentials

For Confluent Cloud deployments, set these environment variables:

```bash
export CONFLUENT_CLOUD_API_KEY=<your-key>
export CONFLUENT_CLOUD_API_SECRET=<your-secret>
export TF_VAR_org_id=<your-org-id>
```

Get your API keys from: https://docs.confluent.io/cloud/current/security/authenticate/workload-identities/service-accounts/api-keys/overview.html

### Demo Parameters

Customize demo behavior with environment variables:

```bash
# Producer settings
PRODUCER_INTERVAL=1        # Seconds between events (default: 1)
PRODUCER_DURATION=10       # How long to produce (default: 10)

# Consumer settings
CONSUMER_GROUP_V1=membership-consumer-v1
CONSUMER_GROUP_V2=membership-consumer-v2

# Configuration file path
CONFIG_PATH=/confluent.properties
```

Example:
```bash
make demo-producer-v1 PRODUCER_INTERVAL=2 PRODUCER_DURATION=60
```

## Workflows

### Complete Confluent Cloud Workflow

```bash
# 1. Set credentials
export CONFLUENT_CLOUD_API_KEY=<your-key>
export CONFLUENT_CLOUD_API_SECRET=<your-secret>
export TF_VAR_org_id=$(confluent organization list -o json | jq -c -r '.[] | select(.is_current)' | jq '.id')

# 2. Initialize
make init

# 3. Setup cloud infrastructure
make setup-cloud

# 4. Build project
make build-v1

# 5. Register schemas
make register-schemas-v1

# 6. Run demo (in separate terminals)
make demo-consumer-v1    # Terminal 1
make demo-producer-v1    # Terminal 2
```

### Schema Evolution Demo Workflow

```bash
# After completing setup above...

# Terminal 1: v1 consumer (for comparison)
make demo-consumer-v1

# Terminal 2: v2 consumer (shows migration rules in action)
make demo-consumer-v2

# Terminal 3: v1 producer (produces v1 events)
make demo-producer-v1
```

Watch how the v2 consumer automatically transforms v1 events using migration rules!

## Teardown

### Confluent Cloud

```bash
make teardown-cloud
```

This destroys all Terraform-managed resources in Confluent Cloud.

## Troubleshooting

### Check Configuration

```bash
# View current .env settings
cat .env

# Check cloud credentials
make check-cloud-credentials

# Check cloud resource status
make cloud-status
```

### Reset and Reconfigure

```bash
# Remove configuration and start over
make clean-env
make init
```

### View Logs

```bash
# Demo status
make demo-status
```

### Common Issues

**"CONFLUENT_CLOUD_API_KEY not set"**
- Export your Confluent Cloud credentials before running `make setup-cloud`

**Schema registration fails**
- Ensure Schema Registry is available (`make cloud-status`)
- Check connection properties in `shared/src/main/resources/confluent.properties`

## Additional Resources

- [Confluent Cloud API Keys](https://docs.confluent.io/cloud/current/security/authenticate/workload-identities/service-accounts/api-keys/overview.html)
- [Schema Registry Documentation](https://docs.confluent.io/platform/current/schema-registry/index.html)
- [Migration Rules](https://docs.confluent.io/platform/current/schema-registry/fundamentals/schema-evolution.html)

## Original Setup Script

The original `setup.sh` script has been replaced by this modular Makefile system. The functionality is preserved but organized into logical, reusable targets.
