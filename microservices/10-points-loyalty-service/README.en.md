# Points Loyalty Service

The Points Loyalty Service is a microservice responsible for the customer loyalty program of the ski resort management system.

## Overview

This service provides the following loyalty features:

- **Point Management**: Granting, using, managing balances, and tracking history of points
- **Membership Tiers**: Tier evaluation, benefit management, and rank-up notifications
- **Benefit Management**: Tier-specific benefits, limited-time benefits, and personalized benefits
- **Campaigns**: Bonus points, event-linked promotions, and referral programs
- **Analysis Functions**: Customer lifetime value, usage patterns, and effectiveness measurement
- **Integration Functions**: Integration with external point services and partner companies

## Technology Stack

- **Jakarta EE 11**: Enterprise Java framework
- **Java 21 LTS**: Programming language
- **WildFly 31.0.1**: Application server
- **PostgreSQL**: Main database
- **Redis**: Cache and real-time updates
- **Apache Kafka**: Event streaming
- **MicroProfile Config**: Configuration management
- **MicroProfile Health**: Health checks

## Architecture

```text
┌─────────────────────────────────────────────────────────┐
│                Points Loyalty Service                   │
├─────────────────────────────────────────────────────────┤
│  REST Layer (JAX-RS)                                   │
│  ├─ PointsResource                                      │
│  ├─ LoyaltyResource                                     │
│  └─ Exception Handlers                                  │
├─────────────────────────────────────────────────────────┤
│  Service Layer                                          │
│  ├─ PointsService                                       │
│  ├─ LoyaltyService                                      │
│  └─ RewardService                                       │
├─────────────────────────────────────────────────────────┤
│  Business Logic Layer                                   │
│  ├─ PointCalculationEngine                              │
│  ├─ TierEvaluationEngine                                │
│  └─ RewardEligibilityEngine                             │
├─────────────────────────────────────────────────────────┤
│  Repository Layer                                       │
│  ├─ PointsAccountRepository                             │
│  ├─ TransactionRepository                               │
│  └─ LoyaltyTierRepository                               │
├─────────────────────────────────────────────────────────┤
│  Entity Layer (JPA)                                     │
│  ├─ PointsAccount                                       │
│  ├─ PointsTransaction                                   │
│  └─ LoyaltyTier                                         │
└─────────────────────────────────────────────────────────┘
```

## Entity Design

### PointsAccount

- Account ID, User ID
- Total points, available points, pending points
- Tier, tier-up date, next evaluation date
- Cumulative purchase amount, last activity date

### PointsTransaction

- Transaction ID, Account ID, related Order ID
- Transaction type (earn, redeem, expire, adjust)
- Number of points, transaction date/time
- Description, expiration date

### LoyaltyTier

- Tier ID, tier name, tier level
- Requirements (purchase amount, points, number of transactions)
- Benefits (point multiplier, discount rate, special services)
- Icon, description

## API Endpoints

### Points Management API

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| GET | `/points/accounts/{userId}` | Get point balance |
| POST | `/points/accounts/{userId}/earn` | Grant points |
| POST | `/points/accounts/{userId}/redeem` | Redeem points |
| GET | `/points/accounts/{userId}/transactions` | Get point history |

### Loyalty Management API

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| GET | `/loyalty/tiers` | Get tier list |
| GET | `/loyalty/users/{userId}/tier` | Get user tier |
| GET | `/loyalty/users/{userId}/benefits` | Get available benefits |
| POST | `/loyalty/users/{userId}/evaluate` | Re-evaluate tier |

### Reward Management API

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| GET | `/rewards/available` | Get available rewards list |
| POST | `/rewards/{rewardId}/claim` | Claim reward |
| GET | `/rewards/history/{userId}` | Get reward usage history |

## Security Features

### Point Security

- Encryption of point transactions
- Fraud detection
- Tamper-proofing of transaction logs
- Double-spending prevention

### Access Control

- Access limited to the user themselves
- Administrator privileges (point adjustments)
- Recording of audit logs

### Data Integrity

- Point balance integrity check
- Completeness guarantee of transaction history
- Regular balance reconciliation

## Configuration

### Environment Variables

| Variable Name | Description | Default Value |
| :--- | :--- | :--- |
| `DATABASE_URL` | Database connection URL | `jdbc:postgresql://localhost:5432/skiresortdb` |
| `DATABASE_USER` | Database user | `skiresort` |
| `DATABASE_PASSWORD` | Database password | `skiresort` |
| `REDIS_HOST` | Redis host | `localhost` |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka servers | `localhost:9092` |
| `POINTS_EXPIRY_MONTHS` | Point expiration period | `12` |
| `BASE_POINTS_RATE` | Base point accrual rate | `0.01` |
| `TIER_EVALUATION_SCHEDULE` | Tier evaluation schedule | `0 0 1 * *` |

## Database Configuration

### PostgreSQL Configuration

```sql
-- Points account table
CREATE TABLE points_accounts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT UNIQUE NOT NULL,
    total_points BIGINT DEFAULT 0,
    available_points BIGINT DEFAULT 0,
    pending_points BIGINT DEFAULT 0,
    tier_id BIGINT REFERENCES loyalty_tiers(id),
    tier_achieved_date DATE,
    next_tier_evaluation_date DATE,
    lifetime_purchase_amount DECIMAL(12,2) DEFAULT 0,
    last_activity_date DATE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Points transaction table
CREATE TABLE points_transactions (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL REFERENCES points_accounts(id),
    transaction_type VARCHAR(20) NOT NULL, -- EARN, REDEEM, EXPIRE, ADJUST
    points_amount BIGINT NOT NULL,
    order_id BIGINT,
    description VARCHAR(500),
    expiry_date DATE,
    transaction_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100)
);

-- Loyalty tier table
CREATE TABLE loyalty_tiers (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    level_order INTEGER NOT NULL,
    min_purchase_amount DECIMAL(12,2) NOT NULL,
    min_points BIGINT,
    min_transactions INTEGER,
    points_multiplier DECIMAL(3,2) DEFAULT 1.00,
    discount_percentage DECIMAL(5,2) DEFAULT 0,
    special_benefits JSONB,
    icon_url VARCHAR(500),
    description TEXT,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

## Event Integration

### Published Events

- `PointsEarnedEvent` - When points are earned
- `PointsRedeemedEvent` - When points are redeemed
- `TierUpgradedEvent` - When a tier is upgraded
- `PointsExpiredEvent` - When points expire

### Consumed Events

- Purchase completion notification from the Order Management Service
- Payment completion notification from the Payment Service
- Member registration notification from the User Management Service

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
cp target/points-loyalty-service.war $WILDFLY_HOME/standalone/deployments/
```

### Docker Run

```bash
# Run with Docker Compose
docker-compose up points-loyalty-service
```

## API Usage Examples

### Get Point Balance

```bash
curl -X GET "http://localhost:8089/points/accounts/123" \
  -H "Authorization: Bearer your_jwt_token"
```

### Grant Points

```bash
curl -X POST http://localhost:8089/points/accounts/123/earn \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer your_jwt_token" \
  -d '{
    "points": 500,
    "orderId": 12345,
    "description": "Lift ticket purchase",
    "expiryDate": "2025-01-15"
  }'
```

### Redeem Points

```bash
curl -X POST http://localhost:8089/points/accounts/123/redeem \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer your_jwt_token" \
  -d '{
    "points": 1000,
    "orderId": 12346,
    "description": "Apply discount"
  }'
```

### Get Tier Information

```bash
curl -X GET "http://localhost:8089/loyalty/users/123/tier" \
  -H "Authorization: Bearer your_jwt_token"
```

### Get Available Benefits

```bash
curl -X GET "http://localhost:8089/loyalty/users/123/benefits" \
  -H "Authorization: Bearer your_jwt_token"
```

## Point Granting Rules

### Basic Granting Rules

- Grant 1% of the purchase amount as points
- Bonus multiplier based on tier
- Additional points during special campaigns

### Bonus Points

- First-time use bonus
- Birthday bonus
- Consecutive use bonus
- Friend referral bonus

### Point Expiration

- Normal points: 12 months
- Bonus points: 6 months
- Campaign points: 3 months

## Loyalty Tiers

### Tier System

1.  **Bronze** (from 0 JPY)
    -   Point multiplier: 1.0x
    -   Benefits: None

2.  **Silver** (from 100,000 JPY)
    -   Point multiplier: 1.2x
    -   Benefits: 5% discount

3.  **Gold** (from 300,000 JPY)
    -   Point multiplier: 1.5x
    -   Benefits: 10% discount, priority booking

4.  **Platinum** (from 1,000,000 JPY)
    -   Point multiplier: 2.0x
    -   Benefits: 15% discount, exclusive lounge, free upgrades

### Tier Evaluation Criteria

- Cumulative purchase amount over the past 12 months
- Automatic monthly evaluation and update
- Tier downgrade only once a year

## Monitoring and Logging

### Health Check

- `/health` endpoint to check service status
- Database connection status
- Redis/Kafka connection status
- Point balance integrity check

### Metrics

- Point grant rate
- Point redemption rate
- Tier distribution
- Benefit usage rate

### Logs

- Point transaction log
- Tier change log
- Benefit usage log
- Error log

## Security Considerations

### Recommended Settings for Production Environment

1.  **Point Data Protection**
    -   Encryption of transaction data
    -   Tamper-proofing functionality

2.  **Fraud Prevention**
    -   Detection of abnormal transactions
    -   Rate limiting

3.  **Audit Functionality**
    -   Recording of all transactions
    -   Tracking of administrator operations

## Troubleshooting

### Common Issues

1.  **Point Balance Mismatch**
    -   Check transaction history
    -   Run periodic integrity checks

2.  **Tier Update Delay**
    -   Check evaluation schedule
    -   Check batch processing logs

3.  **Performance Issues**
    -   Check cache settings
    -   Optimize indexes

## Future Expansion Plans

- [ ] AI-powered personalized reward recommendations
- [ ] Blockchain point management
- [ ] Expansion of integration with external point services
- [ ] Addition of gamification elements
- [ ] Real-time notification functionality

## For Developers

### Code Structure

```text
src/main/java/
├── com/skiresort/loyalty/
│   ├── entity/          # JPA Entities
│   ├── service/         # Business Logic
│   ├── repository/      # Data Access
│   ├── resource/        # REST Endpoints
│   ├── engine/          # Point/Tier Calculation Engine
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
