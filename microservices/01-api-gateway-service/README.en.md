# API Gateway Service

This is the central API gateway for the ski resort management system. It provides routing, authentication, rate limiting, and other cross-cutting concerns for all microservices.

## Features

- **Request Routing**: Routes incoming requests to the appropriate downstream microservice.
- **Authentication**: JWT token validation and user context extraction.
- **Rate Limiting**: Per-client request rate limiting with configurable limits.
- **Health Monitoring**: Health checks for all downstream services.
- **CORS Support**: Cross-origin resource sharing configuration.
- **Service Discovery**: Dynamic routing based on service availability.

## Technology Stack

- Jakarta EE 11
- Java 21 LTS (with virtual thread support)
- WildFly 31.0.1 Application Server
- MicroProfile 6.1 (Config, Health, Metrics, OpenAPI)
- JWT (JSON Web Tokens) Authentication
- Maven Build Management

## Configuration

Configuration is managed with MicroProfile Config in `META-INF/microprofile-config.properties`:

```properties
# Server Configuration
server.host=0.0.0.0
server.port=8080

# JWT Configuration
jwt.secret=your-256-bit-secret-key
jwt.expiration.hours=24

# Rate Limiting
gateway.ratelimit.default.requests=100
gateway.ratelimit.auth.requests=200
gateway.ratelimit.window.seconds=60

# Service URLs
services.user.url=http://localhost:8081
services.product.url=http://localhost:8082
# ... other services
```

## API Routes

The gateway routes requests based on path prefixes:

- `/users/*` → User Management Service (port 8081)
- `/api/v1/products/*` → Product Catalog Service (port 8083)
- `/api/v1/categories/*` → Product Catalog Service (port 8083)
- `/products/*` → Product Catalog Service (port 8083) (deprecated path)
- `/categories/*` → Product Catalog Service (port 8083) (deprecated path)
- `/auth/*` → Authentication Service (port 8084)
- `/inventory/*` → Inventory Management Service (port 8085)
- `/orders/*` → Order Management Service (port 8086)
- `/payments/*` → Payment Service (port 8087)
- `/cart/*` → Shopping Cart Service (port 8088)
- `/coupons/*`, `/discounts/*` → Coupon & Discount Service (port 8089)
- `/points/*`, `/loyalty/*` → Points & Loyalty Service (port 8090)
- `/ai/*`, `/support/*` → AI Support Service (port 8091)

## Rate Limiting

Rate limiting is implemented with the following features:

- Per-client tracking (user ID or IP address)
- Configurable limits per endpoint category
- Sliding window algorithm
- Rate limit headers in responses

## Authentication

JWT authentication includes:

- Token validation on protected endpoints
- User context extraction and forwarding
- Public endpoints (health checks, authentication endpoints)
- Forwarding of authentication headers to downstream services

## Product Catalog Service Endpoints

The API gateway routes the following Product Catalog Service endpoints:

### Category Management (10 endpoints)

- `GET /api/v1/categories` - Get all categories
- `GET /api/v1/categories/root` - Get root categories
- `GET /api/v1/categories/main` - Get main categories
- `GET /api/v1/categories/path` - Get category by path
- `GET /api/v1/categories/{categoryId}` - Get category details
- `GET /api/v1/categories/{categoryId}/children` - Get child categories
- `GET /api/v1/categories/level/{level}` - Get categories by level
- `GET /api/v1/categories/{categoryId}/subcategories` - Get subcategories
- `GET /api/v1/categories/{categoryId}/products` - Get products in a category
- `GET /api/v1/categories/{categoryId}/subcategories/products` - Get products by subcategory

### Product Management (9 endpoints)

- `GET /api/v1/products` - List and search products
- `GET /api/v1/products/featured` - Get featured products
- `GET /api/v1/products/{productId}` - Get product details
- `GET /api/v1/products/sku/{sku}` - Get product by SKU
- `GET /api/v1/products/category/{categoryId}` - Get products by category
- `GET /api/v1/products/brand/{brandId}` - Get products by brand
- `POST /api/v1/products` - Create a product
- `PUT /api/v1/products/{productId}` - Update a product
- `DELETE /api/v1/products/{productId}` - Delete a product

### System Endpoints

- `GET /q/health` - Health check
- `GET /q/openapi` - Get OpenAPI specification

## Health Check

Multiple health check endpoints:

- `/health` - MicroProfile Health check
- `/health/services` - Detailed status of all downstream services
- Liveness and Readiness probes

## Build and Run

```bash
# Build the project
mvn clean compile

# Run tests
mvn test

# Package the WAR file
mvn package

# Deploy to WildFly
mvn wildfly:deploy
```

## Development

The service includes comprehensive unit tests and follows Jakarta EE best practices:

- Constructor injection over field injection
- Proper exception handling and logging
- Configurable timeouts and retry logic
- Clear separation of concerns

## Monitoring

The gateway provides metrics and monitoring through:

- MicroProfile Metrics
- Structured logging
- Service health tracking
- Request/response time measurement

## Security Considerations

- JWT secret key must be changed for production
- HTTPS should be enforced in production
- Rate limiting prevents abuse
- Input validation on all proxied requests
- Proper CORS configuration for web clients
