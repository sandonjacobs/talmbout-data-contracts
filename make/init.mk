# Initialization
# ===============

.PHONY: init clean-env

init: ## Show prerequisites and next steps for Confluent Cloud setup
	@echo "Data Contracts Demo - Initialization"
	@echo "====================================="
	@echo ""
	@echo "This demo runs against Confluent Cloud."
	@echo ""
	@echo "📋 Prerequisites:"
	@echo "  - Confluent Cloud account"
	@echo "  - API Keys (Cloud API Key and Secret)"
	@echo "  - Read more: https://docs.confluent.io/cloud/current/security/authenticate/workload-identities/service-accounts/api-keys/overview.html"
	@echo ""
	@echo "Next steps:"
	@echo "  1. Set your Confluent Cloud credentials:"
	@echo "     export CONFLUENT_CLOUD_API_KEY=<your-key>"
	@echo "     export CONFLUENT_CLOUD_API_SECRET=<your-secret>"
	@echo "  2. Run: make setup-cloud"

clean-env: ## Remove .env configuration file
	@if [ -f .env ]; then \
		echo "Removing .env file..."; \
		rm .env; \
		echo "✓ .env removed."; \
	else \
		echo "No .env file found."; \
	fi
