# Coupon Discount Service

The Coupon Discount Service is a microservice responsible for the coupon and discount functions of the ski resort management system.

## Overview

This service provides the following coupon and discount functions:

- **Coupon Management**: Creation, distribution, usage, and expiration management of coupons
- **Discount Rules**: Conditional discounts, seasonal discounts, group discounts
- **Promotions**: Campaign management, special event discounts
- **Usage Restrictions**: Usage count limits, user limits, combination limits
- **Effectiveness Analysis**: Coupon usage rate, sales impact, customer behavior analysis
- **Automatic Distribution**: Birthday coupons, repeater benefits

## Technology Stack

- **Jakarta EE 11**: Enterprise Java framework
- **Java 21 LTS**: Programming language
- **WildFly 31.0.1**: Application server
- **PostgreSQL**: Main database
- **Redis**: Cache and session management
- **Apache Kafka**: Event streaming
- **MicroProfile Config**: Configuration management
- **MicroProfile Health**: Health checks

## Architecture

```text
┌─────────────────────────────────────────────────────────┐
│               Coupon Discount Service                   │
├─────────────────────────────────────────────────────────┤
│  REST Layer (JAX-RS)                                   │
│  ├─ CouponResource                                      │
│  ├─ DiscountResource                                    │
│  └─ Exception Handlers                                  │
├─────────────────────────────────────────────────────────┤
│  Service Layer                                          │
│  ├─ CouponService                                       │
│  ├─ DiscountService                                     │
│  └─ PromotionService                                    │
├─────────────────────────────────────────────────────────┤
│  Rule Engine Layer                                      │
│  ├─ DiscountRuleEngine                                  │
│  ├─ EligibilityChecker                                  │
│  └─ UsageLimitValidator                                 │
├─────────────────────────────────────────────────────────┤
│  Repository Layer                                       │
│  ├─ CouponRepository                                    │
│  ├─ DiscountRuleRepository                              │
│  └─ UsageHistoryRepository                              │
├─────────────────────────────────────────────────────────┤
│  Entity Layer (JPA)                                     │
│  ├─ Coupon                                              │
│  ├─ DiscountRule                                        │
│  └─ UsageHistory                                        │
└─────────────────────────────────────────────────────────┘
```

## Entity Design

### Coupon

- Coupon ID, coupon code, coupon name
- Discount type (fixed amount, percentage, free), discount value
- Validity period (start date, end date)
- Usage restrictions (count, user, combinability)

### DiscountRule

- Rule ID, rule name, rule type
- Application conditions (product, amount, date/time, user attributes)
- Discount calculation method
- Priority, active status

### UsageHistory

- Usage ID, coupon ID, user ID, order ID
- Usage date/time, discount amount
- Usage status (applied, canceled)

## API Endpoints

### Coupon Management API

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| GET | `/coupons` | Get coupon list |
| POST | `/coupons` | Create coupon |
| GET | `/coupons/{couponId}` | Get coupon details |
| PUT | `/coupons/{couponId}` | Update coupon |
| DELETE | `/coupons/{couponId}` | Delete coupon |

### Discount Calculation API

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| POST | `/discounts/calculate` | Calculate discount amount |
| POST | `/discounts/apply` | Apply discount |
| GET | `/discounts/eligible` | Get eligible discounts |

### Coupon Usage API

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| POST | `/coupons/{couponCode}/validate` | Validate coupon |
| POST | `/coupons/{couponCode}/use` | Use coupon |
| GET | `/users/{userId}/coupons` | Get user coupons |

## Security Features

### Coupon Code Security

- Random code generation
- Guess-resistant code format
- Usage count limit
- Fraud detection

### Access Control

- Administrator privileges (create/delete coupons)
- Staff privileges (distribute coupons)
- User privileges (use coupons)

### Audit Functionality

- Coupon usage history
- Discount application log
- Fraud detection log

## Configuration

### Environment Variables

| Variable Name | Description | Default Value |
| :--- | :--- | :--- |
| `DATABASE_URL` | Database connection URL | `jdbc:postgresql://localhost:5432/skiresortdb` |
| `DATABASE_USER` | Database user | `skiresort` |
| `DATABASE_PASSWORD` | Database password | `skiresort` |
| `REDIS_HOST` | Redis host | `localhost` |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka servers | `localhost:9092` |
| `COUPON_CODE_LENGTH` | Coupon code length | `8` |
| `MAX_DISCOUNT_PERCENTAGE` | Maximum discount percentage | `50` |
| `CACHE_TTL_MINUTES` | Cache TTL | `30` |

## Database Configuration

### PostgreSQL Configuration

```sql
-- Coupon table
CREATE TABLE coupons (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(20) UNIQUE NOT NULL,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    discount_type VARCHAR(20) NOT NULL, -- FIXED_AMOUNT, PERCENTAGE, FREE_ITEM
    discount_value DECIMAL(10,2) NOT NULL,
    min_order_amount DECIMAL(10,2),
    max_discount_amount DECIMAL(10,2),
    usage_limit INTEGER,
    used_count INTEGER DEFAULT 0,
    per_user_limit INTEGER DEFAULT 1,
    valid_from TIMESTAMP NOT NULL,
    valid_until TIMESTAMP NOT NULL,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Discount rule table
CREATE TABLE discount_rules (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    rule_type VARCHAR(50) NOT NULL, -- TIME_BASED, QUANTITY_BASED, USER_BASED
    conditions JSONB NOT NULL,
    discount_type VARCHAR(20) NOT NULL,
    discount_value DECIMAL(10,2) NOT NULL,
    priority INTEGER DEFAULT 0,
    is_combinable BOOLEAN DEFAULT false,
    is_active BOOLEAN DEFAULT true,
    valid_from TIMESTAMP,
    valid_until TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Usage history table
CREATE TABLE usage_history (
    id BIGSERIAL PRIMARY KEY,
    coupon_id BIGINT REFERENCES coupons(id),
    rule_id BIGINT REFERENCES discount_rules(id),
    user_id BIGINT NOT NULL,
    order_id BIGINT,
    discount_amount DECIMAL(10,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'APPLIED',
    used_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

## Event Integration

### Published Events

- `CouponUsedEvent` - When a coupon is used
- `DiscountAppliedEvent` - When a discount is applied
- `CouponExpiredEvent` - When a coupon expires
- `PromotionStartedEvent` - When a promotion starts

### Consumed Events

- Discount calculation request from the Order Management Service
- Birthday notification from the User Management Service
- Payment completion notification from the Payment Service

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
cp target/coupon-discount-service.war $WILDFLY_HOME/standalone/deployments/
```

### Docker Run

```bash
# Run with Docker Compose
docker-compose up coupon-discount-service
```

## API Usage Examples

### Create Coupon

```bash
curl -X POST http://localhost:8088/coupons \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer your_jwt_token" \
  -d '{
    "name": "New Year Campaign 20% OFF",
    "description": "Special New Year Campaign",
    "discountType": "PERCENTAGE",
    "discountValue": 20,
    "minOrderAmount": 5000,
    "maxDiscountAmount": 3000,
    "usageLimit": 100,
    "perUserLimit": 1,
    "validFrom": "2024-01-01T00:00:00Z",
    "validUntil": "2024-01-31T23:59:59Z"
  }'
```

### Calculate Discount

```bash
curl -X POST http://localhost:8088/discounts/calculate \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer your_jwt_token" \
  -d '{
    "userId": 123,
    "items": [
      {
        "productId": "lift-ticket-daily",
        "quantity": 2,
        "price": 5000
      }
    ],
    "couponCode": "NEWYEAR20",
    "totalAmount": 10000
  }'
```

### Validate Coupon

```bash
curl -X POST http://localhost:8088/coupons/NEWYEAR20/validate \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer your_jwt_token" \
  -d '{
    "userId": 123,
    "orderAmount": 8000
  }'
```

### Get User Coupons

```bash
curl -X GET "http://localhost:8088/users/123/coupons?status=available" \
  -H "Authorization: Bearer your_jwt_token"
```

## Discount Rule Engine

### Rule Types

1.  **Time-based Discount**
    -   Early bird discount
    -   Off-season discount
    -   Weekday discount

2.  **Quantity Discount**
    -   Multiple purchase discount
    -   Group discount
    -   Bulk purchase discount

3.  **User Attribute Discount**
    -   Student discount
    -   Senior discount
    -   Member rank discount

### Rule Evaluation Order

1.  Evaluate rules with higher priority first
2.  Optimize combination of combinable rules
3.  Apply maximum discount amount limit

## Monitoring and Logging

### Health Check

- `/health` endpoint to check service status
- Database connection status
- Redis/Kafka connection status
- Rule engine status

### Metrics

- Coupon usage rate
- Discount effectiveness (sales increase rate)
- Fraud detection count
- Popular coupon ranking

### Logs

- Coupon usage log
- Discount calculation log
- Fraud detection log
- Error log

## Security Considerations

### Recommended Settings for Production Environment

1.  **Coupon Code Protection**
    -   Generate guess-resistant codes
    -   Strictly enforce usage limits

2.  **Fraud Prevention**
    -   Detect abnormal patterns
    -   Rate limiting

3.  **Data Protection**
    -   Encrypt usage history
    -   Control access permissions

## Troubleshooting

### Common Issues

1.  **Coupon Usage Error**
    -   Check expiration date
    -   Check usage limits

2.  **Discount Calculation Error**
    -   Check rule conditions
    -   Check combination restrictions

3.  **Performance Issues**
    -   Check cache hit rate
    -   Optimize rule evaluation

## Future Expansion Plans

- [ ] AI-powered personalized coupons
- [ ] Location-based coupons
- [ ] Social media integration
- [ ] A/B testing functionality
- [ ] Dynamic pricing adjustment

## For Developers

### Code Structure

```text
src/main/java/
├── com/skiresort/coupon/
│   ├── entity/          # JPA Entities
│   ├── service/         # Business Logic
│   ├── repository/      # Data Access
│   ├── resource/        # REST Endpoints
│   ├── rule/            # Rule Engine
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
- JSON-B

## License

This project is licensed under the MIT License.
