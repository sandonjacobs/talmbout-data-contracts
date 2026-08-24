# Gradle Build Tasks
# ==================

.PHONY: build-v1 build-v2 register-schemas-v1 register-schemas-v2 clean-build test gradle-wrapper

build-v1: ## Build app-schema-v1 module
	@echo "Building app-schema-v1 module..."
	@./gradlew :app-schema-v1:build
	@echo "✓ app-schema-v1 built"

build-v2: ## Build app-schema-v2 module
	@echo "Building app-schema-v2 module..."
	@./gradlew :app-schema-v2:build
	@echo "✓ app-schema-v2 built"

register-schemas-v1: ## Register v1 schemas with Schema Registry
	@echo "Registering v1 schemas with Confluent Cloud Schema Registry..."
	@./gradlew :app-schema-v1:registerSchemasTask
	@echo "✓ v1 schemas registered"

register-schemas-v2: ## Register v2 schemas with Schema Registry
	@echo "Registering v2 schemas with Confluent Cloud Schema Registry..."
	@./gradlew :app-schema-v2:registerSchemasTask
	@echo "✓ v2 schemas registered"

clean-build: ## Clean all build artifacts
	@echo "Cleaning build artifacts..."
	@./gradlew clean
	@echo "✓ Build artifacts cleaned"

test: ## Run all tests
	@echo "Running tests..."
	@./gradlew test
	@echo "✓ Tests complete"

gradle-wrapper: ## Download Gradle wrapper JARs
	@echo "Downloading Gradle wrapper..."
	@./gradlew wrapper
	@echo "✓ Gradle wrapper ready"