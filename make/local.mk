# Local Container Setup
# =====================

.PHONY: setup-local start-local stop-local restart-local logs-local clean-local check-local-runtime

COMPOSE_FILE := docker-compose.yml
COMPOSE_CMD := $(CONTAINER_RUNTIME)-compose

setup-local: check-deployment-type check-container-runtime check-local-runtime ## Setup local Kafka and Schema Registry
	@if [ "$(DEPLOYMENT_TYPE)" != "local" ]; then \
		echo "❌ Not configured for local deployment. Current: $(DEPLOYMENT_TYPE)"; \
		exit 1; \
	fi
	@echo "Setting up Local Kafka Environment"
	@echo "===================================="
	@echo "Container Runtime: $(CONTAINER_RUNTIME)"
	@echo ""
	@if [ ! -f $(COMPOSE_FILE) ]; then \
		echo "Creating docker-compose.yml..."; \
		$(MAKE) create-compose-file; \
	fi
	@$(MAKE) start-local
	@echo ""
	@echo "Waiting for services to be ready..."
	@sleep 10
	@$(MAKE) export-local-config
	@echo ""
	@echo "✓ Local environment setup complete!"
	@echo ""
	@echo "Services available at:"
	@echo "  Kafka Broker:      localhost:9092"
	@echo "  Schema Registry:   http://localhost:8081"
	@echo "  Control Center:    http://localhost:9021"
	@echo ""
	@echo "Next steps:"
	@echo "  1. Build the project: make build-v1"
	@echo "  2. Register v1 schemas: make register-schemas-v1"
	@echo "  3. Run the demo: make demo-v1"

check-local-runtime:
	@echo "Checking $(CONTAINER_RUNTIME) availability..."
	@if ! command -v $(CONTAINER_RUNTIME) >/dev/null 2>&1; then \
		echo "❌ $(CONTAINER_RUNTIME) not found. Please install it first."; \
		exit 1; \
	fi
	@if ! command -v $(COMPOSE_CMD) >/dev/null 2>&1; then \
		echo "❌ $(COMPOSE_CMD) not found. Please install it first."; \
		exit 1; \
	fi
	@echo "✓ $(CONTAINER_RUNTIME) and $(COMPOSE_CMD) found"

create-compose-file:
	@echo "Creating docker-compose.yml..."
	@cat > $(COMPOSE_FILE) << 'EOF'\n\
version: '3.8'\n\
\n\
services:\n\
  zookeeper:\n\
    image: confluentinc/cp-zookeeper:7.6.0\n\
    hostname: zookeeper\n\
    container_name: zookeeper\n\
    ports:\n\
      - "2181:2181"\n\
    environment:\n\
      ZOOKEEPER_CLIENT_PORT: 2181\n\
      ZOOKEEPER_TICK_TIME: 2000\n\
\n\
  broker:\n\
    image: confluentinc/cp-kafka:7.6.0\n\
    hostname: broker\n\
    container_name: broker\n\
    depends_on:\n\
      - zookeeper\n\
    ports:\n\
      - "9092:9092"\n\
      - "9101:9101"\n\
    environment:\n\
      KAFKA_BROKER_ID: 1\n\
      KAFKA_ZOOKEEPER_CONNECT: 'zookeeper:2181'\n\
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT\n\
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://broker:29092,PLAINTEXT_HOST://localhost:9092\n\
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1\n\
      KAFKA_TRANSACTION_STATE_LOG_MIN_ISR: 1\n\
      KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: 1\n\
      KAFKA_GROUP_INITIAL_REBALANCE_DELAY_MS: 0\n\
      KAFKA_JMX_PORT: 9101\n\
      KAFKA_JMX_HOSTNAME: localhost\n\
\n\
  schema-registry:\n\
    image: confluentinc/cp-schema-registry:7.6.0\n\
    hostname: schema-registry\n\
    container_name: schema-registry\n\
    depends_on:\n\
      - broker\n\
    ports:\n\
      - "8081:8081"\n\
    environment:\n\
      SCHEMA_REGISTRY_HOST_NAME: schema-registry\n\
      SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS: 'broker:29092'\n\
      SCHEMA_REGISTRY_LISTENERS: http://0.0.0.0:8081\n\
\n\
  control-center:\n\
    image: confluentinc/cp-enterprise-control-center:7.6.0\n\
    hostname: control-center\n\
    container_name: control-center\n\
    depends_on:\n\
      - broker\n\
      - schema-registry\n\
    ports:\n\
      - "9021:9021"\n\
    environment:\n\
      CONTROL_CENTER_BOOTSTRAP_SERVERS: 'broker:29092'\n\
      CONTROL_CENTER_SCHEMA_REGISTRY_URL: "http://schema-registry:8081"\n\
      CONTROL_CENTER_REPLICATION_FACTOR: 1\n\
      CONTROL_CENTER_INTERNAL_TOPICS_PARTITIONS: 1\n\
      CONTROL_CENTER_MONITORING_INTERCEPTOR_TOPIC_PARTITIONS: 1\n\
      CONFLUENT_METRICS_TOPIC_REPLICATION: 1\n\
      PORT: 9021\n\
EOF
	@echo "✓ docker-compose.yml created"

start-local: ## Start local Kafka containers
	@echo "Starting local containers with $(CONTAINER_RUNTIME)..."
	@$(COMPOSE_CMD) up -d
	@echo "✓ Containers started"

stop-local: ## Stop local Kafka containers
	@echo "Stopping local containers..."
	@$(COMPOSE_CMD) stop
	@echo "✓ Containers stopped"

restart-local: ## Restart local Kafka containers
	@$(MAKE) stop-local
	@$(MAKE) start-local

logs-local: ## Show logs from local containers
	@$(COMPOSE_CMD) logs -f

clean-local: ## Remove local containers and volumes
	@echo "⚠️  WARNING: This will remove all containers and volumes!"
	@read -p "Are you sure? [y/N]: " confirm; \
	if [ "$$confirm" = "y" ] || [ "$$confirm" = "Y" ]; then \
		$(COMPOSE_CMD) down -v; \
		echo "✓ Containers and volumes removed"; \
	else \
		echo "Cancelled."; \
	fi

teardown-local: clean-local ## Teardown local environment (alias for clean-local)

export-local-config: ## Export local connection properties
	@echo "Exporting local connection properties..."
	@mkdir -p shared/src/main/resources
	@cat > shared/src/main/resources/confluent.properties << 'EOF'\n\
bootstrap.servers=localhost:9092\n\
schema.registry.url=http://localhost:8081\n\
EOF
	@echo "✓ Configuration exported to shared/src/main/resources/confluent.properties"

local-status: ## Show status of local containers
	@$(COMPOSE_CMD) ps
