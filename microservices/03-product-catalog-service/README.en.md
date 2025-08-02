# Product Catalog Service

Product catalog management service for the ski equipment shop.

## Overview

The Product Catalog Service is a microservice responsible for managing the product catalog of the ski equipment sales shop site.

### Key Features

- Product information management (registration, update, deletion)
- Hierarchical management of categories and brands
- Product search and filtering
- Provision of product detail information
- Price management

## Technical Specifications

- **Java**: 21 LTS
- **Framework**: Quarkus 3.8.1
- **Database**: PostgreSQL 16
- **Build Tool**: Maven
- **Container**: Docker

## Development Environment Setup

### Prerequisites

- Java 21
- Docker & Docker Compose
- Maven 3.9+

### Startup Method

#### 1. Startup with Docker Compose

```bash
# Execute in the project root
docker-compose up -d

# Check logs
docker-compose logs -f product-catalog-service
```

#### 2. Local Development Mode

```bash
# Start only PostgreSQL with Docker Compose
docker-compose up -d postgres

# Start in Quarkus development mode
./mvnw quarkus:dev
```

### Access Information

- **API**: http://localhost:8083
- **OpenAPI UI**: http://localhost:8083/q/swagger-ui/
- **Health Check**: http://localhost:8083/q/health
- **Metrics**: http://localhost:8083/q/metrics

## API Endpoints

### Product Management

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/v1/products` | Product list/search |
| GET | `/api/v1/products/{id}` | Get product details |
| POST | `/api/v1/products` | Register product |
| PUT | `/api/v1/products/{id}` | Update product |
| DELETE | `/api/v1/products/{id}` | Delete product |

### Category Management

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/v1/categories` | Get category list |
| GET | `/api/v1/categories/{id}` | Get category details |
| POST | `/api/v1/categories` | Register category |
| PUT | `/api/v1/categories/{id}` | Update category |

### Brand Management

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/v1/brands` | Get brand list |
| GET | `/api/v1/brands/{id}` | Get brand details |
| POST | `/api/v1/brands` | Register brand |

## Test Execution

```bash
# Unit tests
mvn test

# Integration tests
mvn verify

# Generate test coverage report
mvn jacoco:report
```

## Build and Deploy

### Generate JAR file

```bash
mvn clean package
```

### Build Docker Image

```bash
mvn clean package -Dquarkus.container-image.build=true
```

### Build Native Image

```bash
mvn clean package -Pnative
```

## Configuration

Application settings are managed in `src/main/resources/application.yml`.

### Environment Variables

| Variable Name | Description | Default Value |
|--------|------|-------------|
| `QUARKUS_DATASOURCE_JDBC_URL` | PostgreSQL connection URL | `jdbc:postgresql://localhost:5432/product_catalog` |
| `QUARKUS_DATASOURCE_USERNAME` | DB username | `postgres` |
| `QUARKUS_DATASOURCE_PASSWORD` | DB password | `postgres` |
| `QUARKUS_HTTP_PORT` | HTTP port | `8083` |

## License

MIT License
