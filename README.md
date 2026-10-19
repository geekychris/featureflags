<p align="center">
  <img src="assets/logo.png" alt="Featureflags logo" width="180">
</p>

# Feature Flag Manager

A robust service for managing feature flags with a REST API, MySQL persistence, and Memcached integration.

## Overview

This project provides a complete feature flag management solution with the following components:

- **Feature Flag Service**: A Spring Boot service that manages feature flags
- **Java Client**: A Spring Boot client component for consuming feature flags
- **Go Client**: A Go language client for consuming feature flags

Feature flags are name-value pairs associated with an ID. There can be multiple feature flags per ID, and they can be added, updated, or removed through the REST API.

## Architecture

The system consists of:

- **Spring Boot Backend**: Provides REST APIs for managing feature flags
- **MySQL Database**: Persistent storage for feature flags
- **Memcached**: Cache layer for fast access to feature flags
- **Clients**: Libraries for accessing feature flags from Java and Go applications

## Getting Started

### Prerequisites

- Docker and Docker Compose
- Java 17+ (for development)
- Maven 3.6+ (for development)
- Go 1.19+ (for Go client development)

### Running the Service

Using Docker Compose:

```bash
docker-compose up -d
```

This will start:
- The Feature Flag Service on port 8080
- MySQL database
- Memcached instance

### Configuration

The service can be configured through environment variables or application properties:

```properties
# Database configuration
spring.datasource.url=jdbc:mysql://localhost:3306/featureflags
spring.datasource.username=root
spring.datasource.password=password

# Memcached configuration
memcached.servers=localhost:11211
memcached.expiration=3600
```

## REST API

### Feature Flag Management

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/featureflags/{id}` | GET | Get all feature flags for an ID |
| `/api/featureflags/{id}/{name}` | GET | Get a specific feature flag |
| `/api/featureflags/{id}/{name}` | POST | Create or update a feature flag |
| `/api/featureflags/{id}/{name}` | DELETE | Delete a feature flag |
| `/api/featureflags/publish/{id}` | POST | Publish feature flags for an ID to Memcached |
| `/api/featureflags/publish/all` | POST | Publish all feature flags to Memcached |

## Clients

### Java Client

Add the client dependency to your pom.xml:

```xml
<dependency>
    <groupId>com.example</groupId>
    <artifactId>feature-flag-client</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

Usage:

```java
@Autowired
private FeatureFlagClient featureFlagClient;

// Get feature flags for one ID
Map<String, String> flags = featureFlagClient.getFeatureFlags("user123");

// Get and merge feature flags for multiple IDs
Map<String, String> mergedFlags = featureFlagClient.getFeatureFlags("user123", "group456");

// Check if a feature is enabled
boolean isEnabled = Boolean.parseBoolean(flags.getOrDefault("feature-name", "false"));
```

### Go Client

Usage:

```go
import "github.com/example/featureflag/client"

// Create a client
client := featureflag.NewClient([]string{"localhost:11211"})

// Get feature flags for one ID
flags, err := client.GetFeatureFlags("user123")

// Get and merge feature flags for multiple IDs
mergedFlags, err := client.GetFeatureFlags("user123", "group456")

// Check if a feature is enabled
isEnabled := flags["feature-name"] == "true"
```

## Development

### Building the Project

```bash
mvn clean install
```

### Running Tests

```bash
mvn test
```

## License

This project is licensed under the MIT License - see the LICENSE file for details.

# featureflags
