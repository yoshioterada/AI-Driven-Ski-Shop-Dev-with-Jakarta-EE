# Shopping Cart Service

A Quarkus-based microservice that manages shopping cart functionality in the ski equipment e-commerce platform.

## Features

- **Cart Management**: Create, update, and manage shopping carts
- **Item Operations**: Add, remove, and update quantities of products
- **Session Support**: Guest cart management with session tracking
- **Cart Merging**: Integration of guest and user carts upon login
- **Real-time Updates**: WebSocket support for live cart updates
- **Caching**: Redis-based caching for performance improvement
- **Event-Driven**: Kafka integration for microservice communication
- **Resilience**: Circuit breaker, retry, and fallback patterns
- **Monitoring**: Prometheus metrics and distributed tracing

## Technology Stack

- **Framework**: Quarkus 3.15.1 (Jakarta EE 11 compatible)
- **Runtime**: Java 21 LTS
- **Database**: PostgreSQL 16 (with Flyway migrations)
- **Cache**: Redis 7.2
- **Messaging**: Apache Kafka
- **Monitoring**: Prometheus, Jaeger Tracing
- **API**: REST with OpenAPI documentation
- **Real-time**: WebSocket endpoint

## Quick Start

### Prerequisites

- Java 21 LTS
- Maven 3.9+
- Docker and Docker Compose

### Development Mode

1.  Start the infrastructure services:

    ```bash
    docker-compose up -d postgres redis kafka zookeeper
    ```

2.  Run the application in development mode:

    ```bash
    ./mvnw quarkus:dev
    ```

The service is available at:

- **API**: <http://localhost:8088>
- **Swagger UI**: <http://localhost:8088/swagger-ui>
- **Health Check**: <http://localhost:8088/q/health>
- **Metrics**: <http://localhost:8088/metrics>

### Production Mode

1.  Build and run with Docker Compose:

    ```bash
    docker-compose up --build
    ```

2.  Or build the JAR file and run:

    ```bash
    ./mvnw clean package
    java -jar target/shopping-cart-service-1.0.0-SNAPSHOT-runner.jar
    ```

## API Endpoints

### Cart Management

- `GET /api/v1/carts/{cartId}` - Get cart by ID
- `GET /api/v1/carts/session/{sessionId}` - Get/create cart by session
- `GET /api/v1/carts/customer/{customerId}` - Get/create cart by customer

### Item Operations

- `POST /api/v1/carts/{cartId}/items` - Add item to cart
- `PUT /api/v1/carts/{cartId}/items/{sku}/quantity` - Update item quantity
- `DELETE /api/v1/carts/{cartId}/items/{sku}` - Remove item from cart
- `DELETE /api/v1/carts/{cartId}/items` - Clear cart

### Advanced Operations

- `POST /api/v1/carts/{guestCartId}/merge/{customerId}` - Merge guest cart
- `POST /api/v1/carts/{cartId}/validate` - Validate cart

### WebSocket

- `WS /api/v1/carts/ws/{cartId}` - Real-time cart updates

## Configuration

Key configuration properties in `application.properties`:

```properties
# Database
quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5433/cartdb
quarkus.datasource.username=cartuser
quarkus.datasource.password=cartpass

# Redis
quarkus.redis.hosts=redis://localhost:6379

# Kafka
kafka.bootstrap.servers=localhost:9092

# External Services
quarkus.rest-client.product-catalog-service.url=http://localhost:8081
quarkus.rest-client.inventory-management-service.url=http://localhost:8082
```

## Database Schema

The service uses PostgreSQL with the following main tables:

- `shopping_carts` - Cart metadata and totals
- `cart_items` - Individual cart items
- `applied_coupons` - Applied discounts and coupons
- `cart_events` - Event sourcing for cart activities

Migrations are managed with Flyway and located in `src/main/resources/db/migration/`.

## Event Integration

### Published Events

- `CartCreatedEvent` - When a new cart is created
- `CartItemAddedEvent` - When an item is added
- `CartItemRemovedEvent` - When an item is removed
- `CartItemQuantityUpdatedEvent` - When quantity is changed
- `CartMergedEvent` - When carts are merged
- `CartCheckedOutEvent` - When checkout is initiated

### Consumed Events

- Product price updates from the Product Catalog Service
- Inventory changes from the Inventory Management Service
- User profile updates from the User Management Service

## Monitoring and Observability

### Health Checks

- **Liveness**: `/q/health/live`
- **Readiness**: `/q/health/ready`

### Metrics

- **Prometheus**: `/metrics`
- Custom cart-specific metrics:
  - Cart creation rate
  - Item add/remove rate
  - Cache hit rate
  - WebSocket connection count

### Distributed Tracing

- **Jaeger**: Automatic trace collection for all requests
- Custom spans for external service calls
- Correlation between microservices

## Caching Strategy

Redis is used for:

- **Cart Summary**: Frequently accessed cart data (1-hour TTL)
- **Session Mapping**: Mapping guest carts to sessions (8-hour TTL)
- **Temporary Operations**: Locking for concurrent operations (1-minute TTL)

## Resilience Patterns

- **Circuit Breaker**: Protection from external service failures
- **Retry**: Automatic retries for transient failures
- **Fallback**: Graceful degradation when services are unavailable
- **Timeout**: Prevention of hanging requests

## Testing

### Unit Tests

```bash
./mvnw test
```

### Integration Tests

```bash
./mvnw verify
```

### TestContainers

Integration tests use TestContainers for:

- PostgreSQL database
- Redis cache
- Kafka messaging

## Container Deployment

### Docker Build

```bash
docker build -t shopping-cart-service .
```

### Native Build (GraalVM)

```bash
./mvnw package -Pnative -Dquarkus.native.container-build=true
docker build -f Dockerfile.native -t shopping-cart-service:native .
```

## External Service Integration

The service integrates with:

1.  **Product Catalog Service** (port 8081)
    -   Product validation and pricing
    -   Product availability check

2.  **Inventory Management Service** (port 8082)
    -   Inventory status check
    -   Inventory reservation

3.  **User Management Service** (port 8083)
    -   User authentication and authorization
    -   User profile information

4.  **API Gateway Service** (port 8080)
    -   Request routing and load balancing
    -   Authentication and rate limiting

## Development

### Development Mode Features

- **Live Reload**: Automatic restart on code changes
- **Dev UI**: Available at `/q/dev`
- **H2 Console**: Database inspection in development mode
- **Debug Logging**: Detailed logs for development

### Code Structure

```text
src/main/java/com/skishop/cart/
├── entity/          # JPA Entities
├── dto/             # Data Transfer Objects
├── resource/        # REST Endpoints
├── service/         # Business Logic
├── event/           # Event Definitions
├── exception/       # Custom Exceptions
└── health/          # Health Check Implementations
```

## Troubleshooting

### Common Issues

1.  **Database Connection Issues**
    -   Ensure PostgreSQL is running
    -   Check connection parameters
    -   Review Flyway migration logs

2.  **Redis Connection Issues**
    -   Ensure Redis is accessible
    -   Check Redis configuration
    -   Verify network connectivity

3.  **Kafka Issues**
    -   Ensure Kafka and Zookeeper are running
    -   Check topic creation
    -   Verify consumer group settings

### Logging

Application logs include:

- Request/response details
- Cache operations
- Event publishing/consumption
- External service calls
- Error traces with correlation IDs

## Performance Considerations

- **Asynchronous Processing**: CompletableFuture for non-blocking operations
- **Connection Pooling**: Optimization of database and Redis connections
- **Caching Strategy**: Multi-layered caching for frequently accessed data
- **Event Streaming**: Kafka for loosely coupled and scalable communication
- **Resource Limits**: JVM and container limit settings

## Security

- **JWT Authentication**: Support for Bearer token authentication
- **Input Validation**: Jakarta Validation for all request parameters
- **SQL Injection Prevention**: Parameterized queries with JPA
- **CORS Support**: Configurable Cross-Origin Resource Sharing

## Future Enhancements

- [ ] Cart recommendation engine integration
- [ ] Advanced caching strategies (multi-region)
- [ ] Machine learning for cart abandonment prediction
- [ ] Enhanced real-time analytics
- [ ] GraphQL API support
- [ ] Advanced security features (OAuth2, rate limiting)
