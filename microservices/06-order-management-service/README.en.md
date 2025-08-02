````markdown
# Order Management Service

This is a microservice responsible for order processing in the ski resort management system.

## Overview

This service provides the following functions:

- Order creation and management
- Adding, updating, and deleting order items
- Order status management (Pending → Confirmed → Paid → Processing → Shipped → Delivered)
- Payment status management
- Recording of order history
- Order cancellation processing

## Technology Stack

- **Java**: 21 LTS
- **Jakarta EE**: 11
- **JPA**: 3.2
- **JAX-RS**: 4.0
- **CDI**: 4.1
- **Bean Validation**: 3.1
- **MicroProfile**: Health Check
- **Database**: PostgreSQL 16
- **Application Server**: WildFly 31.0.1

## Architecture

### Package Structure

```
com.skiresort.order/
├── model/               # Entity classes
│   ├── Order.java
│   ├── OrderItem.java
│   ├── OrderStatus.java
│   ├── PaymentStatus.java
│   ├── OrderAmount.java
│   ├── ShippingAddress.java
│   └── OrderStatusHistory.java
├── repository/          # Data access layer
│   ├── OrderRepository.java
│   ├── OrderItemRepository.java
│   └── OrderStatusHistoryRepository.java
├── service/            # Business logic layer
│   ├── OrderService.java
│   └── OrderNumberService.java
├── controller/         # REST API layer
│   └── OrderController.java
├── exception/          # Exception classes
│   ├── OrderNotFoundException.java
│   └── InvalidOrderStateException.java
└── health/            # Health checks
    ├── OrderServiceLivenessCheck.java
    └── OrderServiceReadinessCheck.java
```

### Database Design

#### orders Table
- Basic order information
- Amount information (subtotal, discount, tax, shipping, total)
- Shipping and billing addresses
- Various date and time information

#### order_items Table
- Order item information
- Product information, quantity, price

#### order_status_history Table
- Order status change history
- Record of who made the change and the reason

## API Endpoints

### Order Management

| HTTP Method | Endpoint | Description |
|-------------|----------|------|
| POST | `/api/orders` | Create order |
| GET | `/api/orders/{orderId}` | Get order |
| GET | `/api/orders/number/{orderNumber}` | Get by order number |
| GET | `/api/orders/customer/{customerId}` | Customer's order list |
| GET | `/api/orders/status/{status}` | Order list by status |

### Order Status Management

| HTTP Method | Endpoint | Description |
|-------------|----------|------|
| PUT | `/api/orders/{orderId}/status` | Change order status |
| PUT | `/api/orders/{orderId}/payment-status` | Change payment status |
| PUT | `/api/orders/{orderId}/cancel` | Cancel order |

### Order Item Management

| HTTP Method | Endpoint | Description |
|-------------|----------|------|
| POST | `/api/orders/{orderId}/items` | Add item |
| PUT | `/api/orders/{orderId}/items/{itemId}` | Update item |
| DELETE | `/api/orders/{orderId}/items/{itemId}` | Delete item |
| GET | `/api/orders/{orderId}/items` | Get item list |

### Order History

| HTTP Method | Endpoint | Description |
|-------------|----------|------|
| GET | `/api/orders/{orderId}/history` | Get order history |

## Business Rules

### Order Status Transition

```
PENDING → CONFIRMED → PAID → PROCESSING → SHIPPED → DELIVERED
    ↓         ↓         ↓         ↓         ↓         ↓
CANCELLED  CANCELLED  CANCELLED  CANCELLED  CANCELLED  RETURNED
```

### Item Change Rules

- Items can only be changed in `PENDING` and `CONFIRMED` statuses.
- Item changes are generally not allowed after the order is confirmed.

### Cancellation Rules

- Cancellation is possible in statuses other than `DELIVERED` and `RETURNED`.
- Status cannot be changed after cancellation.

## Configuration

### Database Connection Settings

```properties
# PostgreSQL connection settings
spring.datasource.url=jdbc:postgresql://localhost:5432/skiresort_order
spring.datasource.username=skiresort_user
spring.datasource.password=skiresort_pass
```

### Application Settings

```properties
# Application settings
app.order.number.prefix=ORD
app.order.expiry.hours=24
```

## Build & Deploy

### Build

```bash
mvn clean compile
mvn package -DskipTests
```

### Deploy

Deploy to WildFly 31.0.1:

```bash
# WildFly CLI
[standalone@localhost:9990 /] deploy target/order-management-service.war
```

## Health Check

### Liveness Check

```bash
curl http://localhost:8080/health/live
```

### Readiness Check

```bash
curl http://localhost:8080/health/ready
```

## Usage Examples

### Create Order

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "550e8400-e29b-41d4-a716-446655440001",
    "orderItems": [{
      "productId": "550e8400-e29b-41d4-a716-446655440301",
      "sku": "SKI-LIFT-001",
      "productName": "1-Day Ski Lift Pass",
      "unitPrice": 5000.00,
      "quantity": 2
    }],
    "shippingAddress": {
      "name": "Taro Yamada",
      "postalCode": "100-0001",
      "address1": "1-1 Chiyoda, Chiyoda-ku, Tokyo",
      "city": "Chiyoda-ku",
      "country": "Japan"
    }
  }'
```

### Change Order Status

```bash
curl -X PUT http://localhost:8080/api/orders/{orderId}/status \
  -H "Content-Type: application/json" \
  -d '{
    "status": "CONFIRMED",
    "changedBy": "admin",
    "reason": "Inventory confirmed"
  }'
```

## For Developers

### Utilizing Java 21's New Features

- **Record Classes**: Define value objects with `OrderAmount`, `ShippingAddress`.
- **Pattern Matching**: Utilize switch expressions for status transition logic.
- **Text Blocks**: Write long SQL statements in a readable way.

### Jakarta EE 11's New Features

- **JPA 3.2**: Automatic generation of UUID types, improved query features.
- **Bean Validation 3.1**: Finer-grained validation control.
- **CDI 4.1**: Enhanced dependency injection features.

## Troubleshooting

### Common Issues

1. **Database Connection Error**
   - Check if the PostgreSQL server is running.
   - Check if the connection settings are correct.

2. **Order Status Change Error**
   - Check if the status transition is invalid.
   - Check the current status of the order.

3. **Item Change Error**
   - Check if the order status allows changes.
   - Check if the item ID is correct.

## License

This project is licensed under the MIT License.

## Contributing

Pull requests and issue reports are welcome.

## Support

For questions or problems, please use GitHub Issues.
````
