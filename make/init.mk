# Initialization and Environment Selection
# ========================================

.PHONY: init check-deployment-type check-container-runtime setup-env

init: ## Initialize the project (prompts for deployment type)
	@echo "Data Contracts Demo - Initialization"
	@echo "====================================="
	@echo ""
	@if [ -f .env ]; then \
		echo "⚠️  .env file already exists. Remove it to reinitialize."; \
		echo "Current configuration:"; \
		cat .env; \
		exit 1; \
	fi
	@$(MAKE) setup-env

setup-env:
	@echo "Select deployment type:"
	@echo "  1) Confluent Cloud"
	@echo "  2) Local containers"
	@echo ""
	@read -p "Enter choice [1-2]: " choice; \
	case $$choice in \
		1) \
			echo "DEPLOYMENT_TYPE=cloud" > .env; \
			echo ""; \
			echo "✓ Configured for Confluent Cloud"; \
			echo ""; \
			echo "📋 Prerequisites:"; \
			echo "  - Confluent Cloud account"; \
			echo "  - API Keys (Cloud API Key and Secret)"; \
			echo "  - Read more: https://docs.confluent.io/cloud/current/security/authenticate/workload-identities/service-accounts/api-keys/overview.html"; \
			echo ""; \
			echo "Next steps:"; \
			echo "  1. Set your Confluent Cloud credentials:"; \
			echo "     export CONFLUENT_CLOUD_API_KEY=<your-key>"; \
			echo "     export CONFLUENT_CLOUD_API_SECRET=<your-secret>"; \
			echo "  2. Run: make setup-cloud"; \
			;; \
		2) \
			echo ""; \
			echo "Select container runtime:"; \
			echo "  1) Docker"; \
			echo "  2) Podman"; \
			echo ""; \
			read -p "Enter choice [1-2]: " runtime_choice; \
			case $$runtime_choice in \
				1) \
					echo "DEPLOYMENT_TYPE=local" > .env; \
					echo "CONTAINER_RUNTIME=docker" >> .env; \
					echo ""; \
					echo "✓ Configured for local deployment with Docker"; \
					;; \
				2) \
					echo "DEPLOYMENT_TYPE=local" > .env; \
					echo "CONTAINER_RUNTIME=podman" >> .env; \
					echo ""; \
					echo "✓ Configured for local deployment with Podman"; \
					;; \
				*) \
					echo "Invalid choice. Exiting."; \
					exit 1; \
					;; \
			esac; \
			echo ""; \
			echo "Next steps:"; \
			echo "  1. Run: make setup-local"; \
			;; \
		*) \
			echo "Invalid choice. Exiting."; \
			exit 1; \
			;; \
	esac
	@echo ""
	@echo "Configuration saved to .env"

check-deployment-type:
	@if [ -z "$(DEPLOYMENT_TYPE)" ]; then \
		echo "❌ DEPLOYMENT_TYPE not set. Run 'make init' first."; \
		exit 1; \
	fi

check-container-runtime:
	@if [ "$(DEPLOYMENT_TYPE)" = "local" ] && [ -z "$(CONTAINER_RUNTIME)" ]; then \
		echo "❌ CONTAINER_RUNTIME not set for local deployment."; \
		exit 1; \
	fi

clean-env: ## Remove .env configuration file
	@if [ -f .env ]; then \
		echo "Removing .env file..."; \
		rm .env; \
		echo "✓ Configuration cleared. Run 'make init' to reconfigure."; \
	else \
		echo "No .env file found."; \
	fi
