# Gradle Build Tasks
# ==================

.PHONY: build-v1 build-v2 register-schemas-v1 register-schemas-v2 download-schemas-v1 download-schemas-v2 bootstrap-v1 bootstrap-v2 clean-build test gradle-wrapper

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
	@./gradlew :schemas:registerSchemasV1
	@echo "✓ v1 schemas registered"

register-schemas-v2: ## Register v2 schemas with Schema Registry
	@echo "Registering v2 schemas with Confluent Cloud Schema Registry..."
	@./gradlew :schemas:registerSchemasV2
	@echo "✓ v2 schemas registered"

download-schemas-v1: ## Download the v1 schema for app-schema-v1
	@echo "Downloading v1 schema for app-schema-v1..."
	@./gradlew :app-schema-v1:downloadSchemasTask
	@echo "✓ v1 schema downloaded"

download-schemas-v2: ## Download the v2 schema (+ references) for app-schema-v2
	@echo "Downloading v2 schema for app-schema-v2..."
	@./gradlew :app-schema-v2:downloadSchemasTask
	@echo "✓ v2 schema downloaded"

bootstrap-v1: register-schemas-v1 build-v1 ## Register + build app-schema-v1 in one step (build downloads the schema automatically)
	@echo "✓ app-schema-v1 ready to run"

bootstrap-v2: register-schemas-v2 build-v2 ## Register + build app-schema-v2 in one step (build downloads the schema automatically)
	@echo "✓ app-schema-v2 ready to run"

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