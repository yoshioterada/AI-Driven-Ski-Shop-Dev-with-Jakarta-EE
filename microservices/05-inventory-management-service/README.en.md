````markdown
# Inventory Management Service

The Inventory Management Service is a microservice responsible for managing the equipment inventory of the ski resort management system.

## Overview

This service provides the following inventory management functions:

- **Equipment Inventory Management**: Tracking inventory of skis, boots, poles, helmets, etc.
- **Reservation Management**: Rental reservations, inventory allocation, availability checks
- **Maintenance Management**: Scheduling regular inspections, repairs, and replacements of equipment
- **Location Management**: Inventory movement between multiple stores and warehouses
- **Size Management**: Inventory by size, fit checks
- **Real-time Inventory**: Instant inventory status updates

## Technology Stack

- **Jakarta EE 11**: Enterprise Java framework
- **Java 21 LTS**: Programming language
- **WildFly 31.0.1**: Application server
- **PostgreSQL**: Main database
- **Redis**: Inventory cache and real-time updates
- **Apache Kafka**: Event streaming
- **MicroProfile Config**: Configuration management
- **MicroProfile Health**: Health checks

## Architecture

```text
┌─────────────────────────────────────────────────────────┐
│              Inventory Management Service                │
├─────────────────────────────────────────────────────────┤
│  REST Layer (JAX-RS)                                   │
│  ├─ InventoryResource                                   │
│  ├─ ReservationResource                                 │
│  └─ Exception Handlers                                  │
├─────────────────────────────────────────────────────────┤
│  Service Layer                                          │
│  ├─ InventoryService                                    │
│  ├─ ReservationService                                  │
│  └─ MaintenanceService                                  │
├─────────────────────────────────────────────────────────┤
│  Repository Layer                                       │
│  ├─ EquipmentRepository                                 │
│  ├─ InventoryItemRepository                             │
│  └─ ReservationRepository                               │
├─────────────────────────────────────────────────────────┤
│  Entity Layer (JPA)                                     │
│  ├─ Equipment                                           │
│  ├─ InventoryItem                                       │
│  └─ Reservation                                         │
└─────────────────────────────────────────────────────────┘
```

## Entity Design

### Equipment (Master Data)

- Equipment ID, name, category, brand
- Specifications (size, weight, applicable level)
- Maintenance information (inspection interval, replacement time)
- Image URL, description

### InventoryItem

- Inventory ID, equipment ID, serial number
- Status (available, rented, in maintenance, disposed)
- Location (store, warehouse, area)
- Size, condition

### Reservation

- Reservation ID, customer ID, equipment ID
- Reservation period (start date/time, end date/time)
- Status (pending, confirmed, canceled, completed)
- Allocated inventory ID

## API Endpoints

### Inventory Management API

| Method | Endpoint | Description |
|---------|---------------|------|
| GET | `/inventory/equipment` | Get equipment list |
| GET | `/inventory/equipment/{equipmentId}` | Get equipment details |
| GET | `/inventory/equipment/{equipmentId}/availability` | Get inventory status |
| PUT | `/inventory/items/{itemId}/status` | Update inventory status |
| POST | `/inventory/items/{itemId}/maintenance` | Record maintenance |

### Reservation Management API

| Method | Endpoint | Description |
|---------|---------------|------|
| POST | `/inventory/reservations` | Create reservation |
| GET | `/inventory/reservations/{reservationId}` | Get reservation details |
| PUT | `/inventory/reservations/{reservationId}/confirm` | Confirm reservation |
| DELETE | `/inventory/reservations/{reservationId}` | Cancel reservation |

### Availability Check API

| Method | Endpoint | Description |
|---------|---------------|------|
| POST | `/inventory/check-availability` | Check availability of multiple items |
| GET | `/inventory/equipment/{equipmentId}/available-dates` | Get available dates |

## Security Features

### Access Control

- Role-based access control (staff, administrator)
- Granular inventory operation permissions
- Access restriction by store

### Data Integrity

- Optimistic locking for concurrent update control
- Transaction management
- Inventory count integrity check

### Audit Log

- Recording of all inventory changes
- History of reservation operations
- Maintenance records

## Configuration

### Environment Variables

| Variable Name | Description | Default Value |
|--------|------|-------------|
| `DATABASE_URL` | Database connection URL | `jdbc:postgresql://localhost:5432/skiresortdb` |
| `DATABASE_USER` | Database user | `skiresort` |
| `DATABASE_PASSWORD` | Database password | `skiresort` |
| `REDIS_HOST` | Redis host | `localhost` |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka server | `localhost:9092` |
| `INVENTORY_CACHE_TTL` | Inventory cache TTL | `300` |
| `RESERVATION_TIMEOUT` | Reservation timeout | `PT30M` |

## Database Configuration

### PostgreSQL Configuration

```sql
-- Equipment master table
CREATE TABLE equipment (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    category VARCHAR(50) NOT NULL,
    brand VARCHAR(100),
    size_range VARCHAR(50),
    skill_level VARCHAR(20),
    daily_rate DECIMAL(10,2),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Inventory items table
CREATE TABLE inventory_items (
    id BIGSERIAL PRIMARY KEY,
    equipment_id BIGINT NOT NULL REFERENCES equipment(id),
    serial_number VARCHAR(100) UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    location VARCHAR(100),
    size VARCHAR(20),
    condition_rating INTEGER DEFAULT 5,
    last_maintenance_date DATE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Reservations table
CREATE TABLE reservations (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    equipment_id BIGINT NOT NULL REFERENCES equipment(id),
    inventory_item_id BIGINT REFERENCES inventory_items(id),
    start_date TIMESTAMP NOT NULL,
    end_date TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    total_amount DECIMAL(10,2),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

## Event Integration

### Published Events

- `InventoryUpdatedEvent` - When inventory status changes
- `ReservationCreatedEvent` - When a reservation is created
- `ReservationConfirmedEvent` - When a reservation is confirmed
- `EquipmentMaintenanceEvent` - When maintenance is performed

### Consumed Events

- Reservation requests from the Order Management Service
- Customer information updates from the User Management Service
- Payment completion notifications from the Payment Service

## Build and Run

### Prerequisites

- Java 21 LTS
- Maven 3.9+
- PostgreSQL 15+
- Redis 7+
- Apache Kafka 3.0+
- WildFly 31.0.1

### Build

```bash
# Maven build
mvn clean compile

# Run tests
mvn test

# Create package
mvn package
```

### Deploy

```bash
# Deploy to WildFly
cp target/inventory-management-service.war $WILDFLY_HOME/standalone/deployments/
```

### Run with Docker

```bash
# Run with Docker Compose
docker-compose up inventory-management-service
```

## API Usage Examples

### Check Inventory Status

```bash
curl -X GET "http://localhost:8084/inventory/equipment/ski-rossignol-x1/availability?startDate=2024-01-15&endDate=2024-01-17" \
  -H "Authorization: Bearer your_jwt_token"
```

### Create Reservation

```bash
curl -X POST http://localhost:8084/inventory/reservations \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer your_jwt_token" \
  -d '{
    "customerId": 123,
    "equipmentId": "ski-rossignol-x1",
    "startDate": "2024-01-15T09:00:00Z",
    "endDate": "2024-01-17T17:00:00Z",
    "size": "170cm"
  }'
```

### Update Inventory Status

```bash
curl -X PUT http://localhost:8084/inventory/items/item-12345/status \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer your_jwt_token" \
  -d '{
    "status": "MAINTENANCE",
    "reason": "Regular inspection",
    "expectedAvailableDate": "2024-01-20"
  }'
```

## Monitoring and Logging

### Health Check

- `/health` endpoint for service status check
- Database connection status
- Redis/Kafka connection status
- External service connection status

### Metrics

- Inventory turnover rate
- Reservation success rate
- Maintenance frequency
- Response time

### Logs

- Inventory change logs
- Reservation operation logs
- Maintenance records
- Error logs

## Security Considerations

### Recommended Settings for Production Environment

1. **Protection of Inventory Data**
   - Database encryption
   - Minimization of access rights

2. **Concurrent Access Control**
   - Optimistic locking
   - Distributed locking (Redis)

3. **Audit Trail**
   - Recording of all operations
   - Retention of change history

## Troubleshooting

### Common Issues

1. **Inventory Discrepancy**
   - Check cache and DB synchronization
   - Check transaction boundaries

2. **Reservation Conflict**
   - Check locking mechanism
   - Adjust retry settings

3. **Performance Issues**
   - Check cache hit rate
   - Optimize queries

## Future Expansion Plans

- [ ] Demand forecasting with AI
- [ ] IoT sensor integration
- [ ] Automated maintenance scheduling
- [ ] Mobile app support
- [ ] RFID tag management

## For Developers

### Code Structure

```text
src/main/java/
├── com/skiresort/inventory/
│   ├── entity/          # JPA Entities
│   ├── service/         # Business Logic
│   ├── repository/      # Data Access
│   ├── resource/        # REST Endpoints
│   ├── event/           # Event Handling
│   └── exception/       # Exception Classes
```

### Dependencies

- Jakarta EE 11 API
- MicroProfile Config
- MicroProfile Health
- Apache Kafka Client
- PostgreSQL JDBC
- Redis Client

## License

This project is licensed under the MIT License.
````
