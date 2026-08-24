# Demo Orchestration
# ==================

.PHONY: demo-v1 demo-v2 demo-producer-v1 demo-producer-v2 demo-consumer-v1 demo-consumer-v2 demo-full demo-reset demo-status

# Configuration
PRODUCER_INTERVAL ?= 1
PRODUCER_DURATION ?= 10
CONSUMER_GROUP_V1 ?= membership-consumer-v1
CONSUMER_GROUP_V2 ?= membership-consumer-v2
CONFIG_PATH ?= /confluent.properties

demo-v1: ## Show instructions for v1 demo
	@echo "Data Contracts Demo - Schema v1"
	@echo "================================"
	@echo ""
	@echo "This will run both producer and consumer for schema v1."
	@echo "Open two terminal windows and run:"
	@echo ""
	@echo "Terminal 1 (Consumer):"
	@echo "  make demo-consumer-v1"
	@echo ""
	@echo "Terminal 2 (Producer):"
	@echo "  make demo-producer-v1"
	@echo ""

demo-v2: ## Show instructions for v2 evolution demo
	@echo "Data Contracts Demo - Schema Evolution (v1 → v2)"
	@echo "================================================="
	@echo ""
	@echo "This demonstrates schema evolution with migration rules."
	@echo "You can run v1 or v2 producers, and v1 or v2 consumers."
	@echo ""
	@echo "Open three terminal windows and run:"
	@echo ""
	@echo "Terminal 1 (v1 Consumer - for comparison):"
	@echo "  make demo-consumer-v1"
	@echo ""
	@echo "Terminal 2 (v2 Consumer - shows migration):"
	@echo "  make demo-consumer-v2"
	@echo ""
	@echo "Terminal 3 (Producer - choose v1 or v2):"
	@echo "  make demo-producer-v1  # or"
	@echo "  make demo-producer-v2"
	@echo ""

demo-producer-v1: ## Run v1 producer
	@echo "Starting schema v1 producer..."
	@echo "Interval: $(PRODUCER_INTERVAL)s, Duration: $(PRODUCER_DURATION)s"
	@echo ""
	@./gradlew :app-schema-v1:run --args="producer -p $(CONFIG_PATH) -i $(PRODUCER_INTERVAL) -d $(PRODUCER_DURATION)"

demo-consumer-v1: ## Run v1 consumer
	@echo "Starting schema v1 consumer..."
	@echo "Consumer Group: $(CONSUMER_GROUP_V1)"
	@echo ""
	@./gradlew :app-schema-v1:run --args="consumer -p $(CONFIG_PATH) -g $(CONSUMER_GROUP_V1)"

demo-producer-v2: ## Run v2 producer
	@echo "Starting schema v2 producer..."
	@echo "Interval: $(PRODUCER_INTERVAL)s, Duration: $(PRODUCER_DURATION)s"
	@echo ""
	@./gradlew :app-schema-v2:run --args="producer -p $(CONFIG_PATH) -i $(PRODUCER_INTERVAL) -d $(PRODUCER_DURATION)"

demo-consumer-v2: ## Run v2 consumer (with migration rules)
	@echo "Starting schema v2 consumer..."
	@echo "Consumer Group: $(CONSUMER_GROUP_V2)"
	@echo ""
	@echo "This consumer uses schema v2 and will automatically transform"
	@echo "v1 events using migration rules in Schema Registry."
	@echo ""
	@./gradlew :app-schema-v2:run --args="consumer -p $(CONFIG_PATH) -g $(CONSUMER_GROUP_V2)"

demo-full: ## Show all demo parameters
	@echo "Full Demo Configuration"
	@echo "======================="
	@echo ""
	@echo "Available parameters (set via environment or command line):"
	@echo "  PRODUCER_INTERVAL  - Seconds between events (default: 1)"
	@echo "  PRODUCER_DURATION  - How long to produce (default: 10)"
	@echo "  CONSUMER_GROUP_V1  - Consumer group for v1 (default: membership-consumer-v1)"
	@echo "  CONSUMER_GROUP_V2  - Consumer group for v2 (default: membership-consumer-v2)"
	@echo "  CONFIG_PATH        - Path to config file (default: /confluent.properties)"
	@echo ""
	@echo "Example:"
	@echo "  make demo-producer-v1 PRODUCER_INTERVAL=2 PRODUCER_DURATION=60"
	@echo ""

demo-reset: ## Reset consumer group offsets
	@echo "Use the Confluent CLI or Cloud Console to reset consumer groups."
	@echo "  confluent kafka consumer-group delete $(CONSUMER_GROUP_V1)"
	@echo "  confluent kafka consumer-group delete $(CONSUMER_GROUP_V2)"

demo-status: ## Show topics and consumer groups
	@echo "Demo Status"
	@echo "==========="
	@echo ""
	@echo "Use Confluent Cloud Console or CLI to view topics and consumer groups."
	@echo "  confluent kafka topic list"
	@echo "  confluent kafka consumer-group list"