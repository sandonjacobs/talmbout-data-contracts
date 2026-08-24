# Demo Orchestration
# ==================

.PHONY: demo-v1 demo-v2 demo-producer-v1 demo-producer-v2 demo-consumer-v1 demo-consumer-v2 demo-full demo-reset demo-status check-demo-ready

# Configuration
# NOTE: the app's -i/--interval flag is milliseconds, not seconds - keep this suffix so it's
# never re-guessed as seconds again (that mismatch made the default a 1ms firehose).
PRODUCER_INTERVAL_MS ?= 1000
PRODUCER_DURATION ?= 10
# Suffixed with a run id so every demo run starts consuming from a brand-new, empty group
# (earliest offset) instead of resuming a prior run's committed offsets.
DEMO_RUN_ID ?= $(shell date +%s)
CONSUMER_GROUP_V1 ?= membership-consumer-v1-$(DEMO_RUN_ID)
CONSUMER_GROUP_V2 ?= membership-consumer-v2-$(DEMO_RUN_ID)
CONFIG_PATH ?= /confluent.properties

check-demo-ready: ## Verify prerequisites before running a producer/consumer
	@if [ ! -f shared/src/main/resources/confluent.properties ]; then \
		echo "❌ shared/src/main/resources/confluent.properties not found."; \
		echo ""; \
		echo "Run these first:"; \
		echo "  make setup-cloud"; \
		echo ""; \
		exit 1; \
	fi

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

demo-producer-v1: check-demo-ready ## Run v1 producer
	@echo "Starting schema v1 producer..."
	@echo "Interval: $(PRODUCER_INTERVAL_MS)ms, Duration: $(PRODUCER_DURATION)s"
	@echo ""
	@./gradlew :app-schema-v1:run --args="producer -p $(CONFIG_PATH) -i $(PRODUCER_INTERVAL_MS) -d $(PRODUCER_DURATION)"

demo-consumer-v1: check-demo-ready ## Run v1 consumer
	@echo "Starting schema v1 consumer..."
	@echo "Consumer Group: $(CONSUMER_GROUP_V1)"
	@echo ""
	@./gradlew :app-schema-v1:run --args="consumer -p $(CONFIG_PATH) -g $(CONSUMER_GROUP_V1)"

demo-producer-v2: check-demo-ready ## Run v2 producer
	@echo "Starting schema v2 producer..."
	@echo "Interval: $(PRODUCER_INTERVAL_MS)ms, Duration: $(PRODUCER_DURATION)s"
	@echo ""
	@./gradlew :app-schema-v2:run --args="producer -p $(CONFIG_PATH) -i $(PRODUCER_INTERVAL_MS) -d $(PRODUCER_DURATION)"

demo-consumer-v2: check-demo-ready ## Run v2 consumer (with migration rules)
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
	@echo "  PRODUCER_INTERVAL_MS - Milliseconds between events (default: 1000)"
	@echo "  PRODUCER_DURATION    - How long to produce, in seconds (default: 10)"
	@echo "  CONSUMER_GROUP_V1    - Consumer group for v1 (default: membership-consumer-v1-<run-id>)"
	@echo "  CONSUMER_GROUP_V2    - Consumer group for v2 (default: membership-consumer-v2-<run-id>)"
	@echo "  CONFIG_PATH          - Path to config file (default: /confluent.properties)"
	@echo ""
	@echo "Each run gets a fresh consumer group id (via DEMO_RUN_ID) so it always starts"
	@echo "reading from the earliest offset, instead of resuming a prior run's progress."
	@echo ""
	@echo "Example:"
	@echo "  make demo-producer-v1 PRODUCER_INTERVAL_MS=2000 PRODUCER_DURATION=60"
	@echo ""

demo-reset: ## Reset consumer group offsets
	@echo "Consumer groups are now uniquely named per run (see DEMO_RUN_ID in demo-full),"
	@echo "so a fresh 'make demo-consumer-v1' / 'make demo-consumer-v2' already starts clean."
	@echo ""
	@echo "To manually delete a specific group instead, use the Confluent CLI:"
	@echo "  confluent kafka consumer-group delete <group-id>"

demo-status: ## Show topics and consumer groups
	@echo "Demo Status"
	@echo "==========="
	@echo ""
	@echo "Use Confluent Cloud Console or CLI to view topics and consumer groups."
	@echo "  confluent kafka topic list"
	@echo "  confluent kafka consumer-group list"