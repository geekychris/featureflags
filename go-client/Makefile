.PHONY: all build test lint clean

# Default target
all: lint test build

# Build the client library
build:
	go build -v ./...

# Run tests with coverage
test:
	go test -v -race -coverprofile=coverage.out ./...
	go tool cover -func=coverage.out

# Run golangci-lint
lint:
	golangci-lint run ./...

# Clean build artifacts
clean:
	rm -f coverage.out
	rm -rf bin/
	go clean

help:
	@echo "Available targets:"
	@echo "  all    - Run lint, test, and build"
	@echo "  build  - Build the client library"
	@echo "  test   - Run tests with coverage"
	@echo "  lint   - Run golangci-lint"
	@echo "  clean  - Clean build artifacts"

