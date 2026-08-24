# Confluent Cloud Setup
# =====================

.PHONY: setup-cloud check-cloud-credentials terraform-init terraform-plan terraform-apply terraform-destroy export-cloud-config teardown-cloud

setup-cloud: check-cloud-credentials ## Setup Confluent Cloud environment
	@echo "Setting up Confluent Cloud Environment..."
	@echo "=========================================="
	@echo ""
	@$(MAKE) terraform-init
	@$(MAKE) terraform-plan
	@$(MAKE) terraform-apply
	@$(MAKE) export-cloud-config
	@echo ""
	@echo "✓ Confluent Cloud setup complete!"
	@echo ""
	@echo "Next steps:"
	@echo "  1. Register + build v1: make bootstrap-v1"
	@echo "  2. Run the demo: make demo-v1"

check-cloud-credentials:
	@echo "Checking Confluent Cloud credentials..."
	@if [ -z "$(CONFLUENT_CLOUD_API_KEY)" ]; then \
		echo "❌ CONFLUENT_CLOUD_API_KEY not set."; \
		echo ""; \
		echo "Please set your credentials:"; \
		echo "  export CONFLUENT_CLOUD_API_KEY=<your-key>"; \
		echo "  export CONFLUENT_CLOUD_API_SECRET=<your-secret>"; \
		echo ""; \
		echo "Read more: https://docs.confluent.io/cloud/current/security/authenticate/workload-identities/service-accounts/api-keys/overview.html"; \
		exit 1; \
	fi
	@if [ -z "$(CONFLUENT_CLOUD_API_SECRET)" ]; then \
		echo "❌ CONFLUENT_CLOUD_API_SECRET not set."; \
		exit 1; \
	fi
	@echo "✓ Credentials found"

terraform-init: ## Initialize Terraform
	@echo "Initializing Terraform..."
	@cd cc-terraform && terraform init

terraform-plan: ## Create Terraform plan
	@echo "Creating Terraform plan..."
	@cd cc-terraform && terraform plan -out "tfplan"

terraform-apply: ## Apply Terraform configuration
	@echo "Applying Terraform configuration..."
	@cd cc-terraform && terraform apply "tfplan"
	@echo "✓ Confluent Cloud resources provisioned"

export-cloud-config: ## Export Confluent Cloud connection properties
	@echo "Exporting Confluent Cloud connection properties..."
	@cd cc-terraform && terraform output -json \
		| jq -r 'to_entries | map( {key: .key|tostring|split("_")|join("."), value: .value} ) | map("\(.key)=\(.value.value)") | .[]' \
		> ../shared/src/main/resources/confluent.properties
	@echo "✓ Configuration exported to shared/src/main/resources/confluent.properties"

terraform-destroy: ## Destroy Terraform resources (use with caution)
	@echo "⚠️  WARNING: This will destroy all Confluent Cloud resources!"
	@read -p "Are you sure? [y/N]: " confirm; \
	if [ "$$confirm" = "y" ] || [ "$$confirm" = "Y" ]; then \
		cd cc-terraform && terraform destroy -auto-approve; \
		echo "✓ Resources destroyed"; \
	else \
		echo "Cancelled."; \
	fi

teardown-cloud: terraform-destroy ## Teardown Confluent Cloud environment (alias for terraform-destroy)

cloud-status: ## Show Confluent Cloud resource status
	@echo "Confluent Cloud Status"
	@echo "======================"
	@cd cc-terraform && terraform show

cloud-output: ## Show Terraform outputs
	@cd cc-terraform && terraform output
