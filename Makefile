.PHONY: help
.DEFAULT_GOAL := help

# Load environment variables from .env if it exists
-include .env
export

# Include modular makefiles
include make/init.mk
include make/confluent-cloud.mk
include make/local.mk
include make/gradle.mk
include make/demo.mk

help: ## Show this help message
	@echo "Data Contracts Demo - Available Commands"
	@echo "========================================"
	@echo ""
	@printf "\033[1mSetup & Initialization:\033[0m\n"
	@grep -h '##' make/init.mk | grep -v '^\#' | \
		awk 'BEGIN {FS = ":.*## "}; {printf "  \033[36m%-28s\033[0m %s\n", $$1, $$2}'
	@echo ""
	@printf "\033[1mConfluent Cloud Infrastructure:\033[0m\n"
	@grep -h '##' make/confluent-cloud.mk | grep -v '^\#' | \
		awk 'BEGIN {FS = ":.*## "}; {printf "  \033[36m%-28s\033[0m %s\n", $$1, $$2}'
	@echo ""
	@printf "\033[1mLocal Container Infrastructure:\033[0m\n"
	@grep -h '##' make/local.mk | grep -v '^\#' | \
		awk 'BEGIN {FS = ":.*## "}; {printf "  \033[36m%-28s\033[0m %s\n", $$1, $$2}'
	@echo ""
	@printf "\033[1mGradle Build Tasks:\033[0m\n"
	@grep -h '##' make/gradle.mk | grep -v '^\#' | \
		awk 'BEGIN {FS = ":.*## "}; {printf "  \033[36m%-28s\033[0m %s\n", $$1, $$2}'
	@echo ""
	@printf "\033[1mDemo Orchestration:\033[0m\n"
	@grep -h '##' make/demo.mk | grep -v '^\#' | \
		awk 'BEGIN {FS = ":.*## "}; {printf "  \033[36m%-28s\033[0m %s\n", $$1, $$2}'
	@echo ""
	@echo "Environment Variables (set in .env or export):"
	@echo "  DEPLOYMENT_TYPE    - 'cloud' or 'local' (default: prompt)"
	@echo "  CONTAINER_RUNTIME  - 'docker' or 'podman' (for local only)"
	@echo ""
