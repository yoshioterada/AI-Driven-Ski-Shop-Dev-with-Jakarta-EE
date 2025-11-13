# Inventory Management Service (在庫管理サービス)

This is the inventory management microservice for the ski shop application, implementing Phase 1 of the detailed implementation plan.

## Overview

The Inventory Management Service is responsible for:
- Managing ski equipment inventory data
- Synchronizing with Product Catalog Service through Kafka events
- Providing APIs for inventory operations (check availability, update stock levels)
- Caching product information for performance
- Supporting concurrent access with optimistic locking

## Technology Stack

- **Framework**: Quarkus 3.8.1
- **Language**: Java 17
- **Database**: PostgreSQL (with H2 for testing)
- **Cache**: Redis
- **Messaging**: Apache Kafka
- **Database Migration**: Flyway
- **API Documentation**: OpenAPI/Swagger
- **Testing**: JUnit 5, Rest Assured

## Phase 1 Features Implemented

✅ **Basic Infrastructure**
- Project structure with Quarkus
- Database schema with Equipment and InventoryItem entities
- PostgreSQL configuration with Flyway migrations
- Redis cache configuration

✅ **Product Catalog Integration**
- Kafka event consumers for Product Catalog events
- Product information caching and synchronization
- Event-driven architecture implementation

✅ **Basic Inventory Management**
- Equipment repository with optimistic locking
- Basic REST API endpoints for inventory operations
- Availability checking functionality
- Stock level updates

✅ **Testing**
- Unit tests for repository layer
- Application service tests
- Test configuration with H2 database

## API Endpoints

### Equipment Management
- `GET /api/v1/inventory/equipment` - List equipment with filtering
- `GET /api/v1/inventory/equipment/{productId}` - Get equipment details
- `GET /api/v1/inventory/equipment/{productId}/availability` - Check availability
- `POST /api/v1/inventory/equipment/{productId}/stock` - Update stock level
- `GET /api/v1/inventory/equipment/search` - Search equipment

### Health and Monitoring
- `GET /api/v1/inventory/health` - Health check
- `GET /health` - Quarkus health check
- `GET /metrics` - Prometheus metrics
- `GET /q/openapi` - OpenAPI specification

## Configuration

### Database Configuration
```properties
quarkus.datasource.db-kind=postgresql
quarkus.datasource.username=inventory
quarkus.datasource.password=inventory
quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/inventory_db
```

### Redis Configuration
```properties
quarkus.redis.hosts=redis://localhost:6379
```

### Kafka Configuration
```properties
kafka.bootstrap.servers=localhost:9092
mp.messaging.incoming.product-events.topic=product-events
mp.messaging.outgoing.inventory-events.topic=inventory-events
```

## Building and Running

### Local Development
```bash
# Run with dev mode (auto-reload)
./mvnw quarkus:dev

# The service will be available at http://localhost:8085
# Swagger UI: http://localhost:8085/q/swagger-ui
```

### Testing
```bash
# Run all tests
./mvnw test

# Run with coverage
./mvnw test jacoco:report
```

### Production Build
```bash
# Build JAR
./mvnw clean package

# Build Docker image
docker build -f Dockerfile -t inventory-management-service .
```

### Docker Compose
```bash
# Start all services (PostgreSQL, Redis, Kafka, Inventory Service)
docker-compose up -d

# View logs
docker-compose logs -f inventory-service

# Stop services
docker-compose down
```

## Database Schema

### Equipment Table
- Stores cached product information from Product Catalog
- Manages available and reserved quantities
- Uses optimistic locking for concurrent updates

### Inventory Items Table
- Individual inventory items with serial numbers
- Tracks condition, maintenance, and rental history
- Links to Equipment as parent aggregate

## Event Integration

### Consumed Events (from Product Catalog)
- `ProductCreatedEvent` - Creates new equipment entry
- `ProductUpdatedEvent` - Updates cached product information
- `ProductDeletedEvent` - Deactivates equipment
- `ProductPriceChangedEvent` - Updates pricing information

### Published Events (to other services)
- `InventoryUpdatedEvent` - Notifies of stock changes
- `LowStockAlertEvent` - Alerts when stock is low
- `StockReservationEvent` - Stock reservation changes

## Future Phases

🔄 **Phase 2 - Stock Reservation System** (Planned)
- Stock reservation entities and logic
- Reservation timeout management  
- Concurrency control improvements

🔄 **Phase 3 - Pricing and Advanced Features** (Planned)
- Dynamic pricing calculation
- Maintenance management
- Advanced search and filtering

🔄 **Phase 4 - Monitoring and Analytics** (Planned)
- Inventory analytics
- Performance monitoring
- Alert management

## Testing the Service

### Manual Testing
```bash
# Check health
curl http://localhost:8085/health

# Get equipment list
curl http://localhost:8085/api/v1/inventory/equipment

# Check availability
curl "http://localhost:8085/api/v1/inventory/equipment/{productId}/availability?quantity=2"

# Update stock
curl -X POST http://localhost:8085/api/v1/inventory/equipment/{productId}/stock \
  -H "Content-Type: application/json" \
  -d '{"quantity": 10, "reason": "PURCHASE", "notes": "New stock arrival"}'
```

### Integration with Product Catalog
The service automatically consumes events from the Product Catalog Service when running with Kafka. Product creation/updates in the catalog will automatically create corresponding inventory entries.

## Monitoring and Observability

- **Health Checks**: Available at `/health`
- **Metrics**: Prometheus metrics at `/metrics`  
- **Logging**: Structured JSON logging in production
- **Tracing**: Ready for distributed tracing integration

## Security

- JWT authentication ready (currently disabled for development)
- Input validation with Bean Validation
- SQL injection protection with parameterized queries
- CORS enabled for development

## Performance Optimizations

- Redis caching for equipment data
- Database indexes on frequently queried columns
- Optimistic locking for concurrent updates
- Connection pooling for database access

This service implements the foundation for the inventory management system and is ready for integration with other microservices in the ski shop application.
