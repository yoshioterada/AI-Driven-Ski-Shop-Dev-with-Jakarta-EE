````markdown
# Payment Service

The Payment Service is a microservice responsible for handling payment processing for the ski resort management system.

## Overview

This service provides the following payment functions:

- **Payment Processing**: Credit cards, electronic money, QR code payments
- **Billing Management**: Calculation of usage fees, creation of statements, issuance of receipts
- **Refund Processing**: Refunds for cancellations, partial refunds
- **Installment Payments**: Credit card installments, deferred payment services
- **Sales Management**: Daily sales, monthly reports, analytical data
- **Security**: PCI DSS compliance, fraud detection

## Technology Stack

- **Jakarta EE 11**: Enterprise Java framework
- **Java 21 LTS**: Programming language
- **WildFly 31.0.1**: Application server
- **PostgreSQL**: Main database
- **Redis**: Session management and caching
- **Apache Kafka**: Event streaming
- **MicroProfile Config**: Configuration management
- **MicroProfile Health**: Health checks

## Architecture

```text
┌─────────────────────────────────────────────────────────┐
│                  Payment Service                        │
├─────────────────────────────────────────────────────────┤
│  REST Layer (JAX-RS)                                   │
│  ├─ PaymentResource                                     │
│  ├─ RefundResource                                      │
│  └─ Exception Handlers                                  │
├─────────────────────────────────────────────────────────┤
│  Service Layer                                          │
│  ├─ PaymentService                                      │
│  ├─ RefundService                                       │
│  └─ FraudDetectionService                               │
├─────────────────────────────────────────────────────────┤
│  Payment Gateway Layer                                  │
│  ├─ CreditCardProcessor                                 │
│  ├─ QRCodeProcessor                                     │
│  └─ ElectronicMoneyProcessor                            │
├─────────────────────────────────────────────────────────┤
│  Repository Layer                                       │
│  ├─ PaymentRepository                                   │
│  ├─ TransactionRepository                               │
│  └─ RefundRepository                                    │
├─────────────────────────────────────────────────────────┤
│  Entity Layer (JPA)                                     │
│  ├─ Payment                                             │
│  ├─ Transaction                                         │
│  └─ RefundRecord                                        │
└─────────────────────────────────────────────────────────┘
```

## Entity Design

### Payment

- Payment ID, Order ID, Customer ID
- Payment amount, currency, payment method
- Status (processing, success, failed, canceled)
- Payment time, update time

### Transaction

- Transaction ID, Payment ID, External Transaction ID
- Payment provider, authorization code
- Fee, tax amount
- Response code, response message

### RefundRecord

- Refund ID, Payment ID, refund amount
- Reason for refund, refund status
- Refund date/time, processor ID

## API Endpoints

### Payment Processing API

| Method | Endpoint | Description |
|---------|---------------|------|
| POST | `/payment/process` | Process payment |
| GET | `/payment/{paymentId}` | Get payment details |
| GET | `/payment/{paymentId}/status` | Check payment status |
| POST | `/payment/{paymentId}/cancel` | Cancel payment |

### Refund Processing API

| Method | Endpoint | Description |
|---------|---------------|------|
| POST | `/payment/{paymentId}/refund` | Process refund |
| GET | `/payment/{paymentId}/refunds` | Get refund history |
| GET | `/refund/{refundId}` | Get refund details |

### Sales Management API

| Method | Endpoint | Description |
|---------|---------------|------|
| GET | `/payment/sales/daily` | Get daily sales |
| GET | `/payment/sales/monthly` | Get monthly sales |
| GET | `/payment/transactions` | Search transaction history |

## Security Features

### PCI DSS Compliance

- Encryption of card information
- Secure communication (TLS 1.3)
- Recording of access logs
- Regular security audits

### Fraud Detection

- Detection of abnormal transaction patterns
- Detection of geographical anomalies
- Detection of amount anomalies
- Blacklist check

### Data Protection

- Pseudonymization of personal information
- Encryption of payment data
- Control of access rights
- Retention of audit trails

## Configuration

### Environment Variables

| Variable Name | Description | Default Value |
|--------|------|-------------|
| `DATABASE_URL` | Database connection URL | `jdbc:postgresql://localhost:5432/skiresortdb` |
| `DATABASE_USER` | Database user | `skiresort` |
| `DATABASE_PASSWORD` | Database password | `skiresort` |
| `REDIS_HOST` | Redis host | `localhost` |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka server | `localhost:9092` |
| `PAYMENT_GATEWAY_URL` | Payment gateway URL | `https://api.payment-gateway.com` |
| `PAYMENT_GATEWAY_API_KEY` | Payment gateway API key | `dummy_api_key` |
| `FRAUD_DETECTION_THRESHOLD` | Fraud detection threshold | `0.8` |

## Database Configuration

### PostgreSQL Configuration

```sql
-- Payments table
CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    customer_id BIGINT NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'JPY',
    payment_method VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    gateway_transaction_id VARCHAR(200),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Transactions table
CREATE TABLE transactions (
    id BIGSERIAL PRIMARY KEY,
    payment_id BIGINT NOT NULL REFERENCES payments(id),
    external_transaction_id VARCHAR(200),
    payment_provider VARCHAR(50),
    authorization_code VARCHAR(100),
    processing_fee DECIMAL(10,2),
    tax_amount DECIMAL(10,2),
    response_code VARCHAR(10),
    response_message TEXT,
    processed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Refund records table
CREATE TABLE refund_records (
    id BIGSERIAL PRIMARY KEY,
    payment_id BIGINT NOT NULL REFERENCES payments(id),
    refund_amount DECIMAL(10,2) NOT NULL,
    reason VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    processed_by BIGINT,
    processed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

## Event Integration

### Published Events

- `PaymentProcessedEvent` - When payment processing is complete
- `PaymentFailedEvent` - When payment fails
- `RefundProcessedEvent` - When refund processing is complete
- `FraudDetectedEvent` - When fraud is detected

### Consumed Events

- Payment requests from the Order Management Service
- Customer information updates from the User Management Service
- Payment initiation from the Shopping Cart Service

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
cp target/payment-service.war $WILDFLY_HOME/standalone/deployments/
```

### Run with Docker

```bash
# Run with Docker Compose
docker-compose up payment-service
```

## API Usage Examples

### Process Payment

```bash
curl -X POST http://localhost:8086/payment/process \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer your_jwt_token" \
  -d '{
    "orderId": 12345,
    "customerId": 678,
    "amount": 15000,
    "currency": "JPY",
    "paymentMethod": "CREDIT_CARD",
    "cardDetails": {
      "number": "4111111111111111",
      "expiryMonth": 12,
      "expiryYear": 2025,
      "cvv": "123",
      "holderName": "TARO YAMADA"
    }
  }'
```

### Check Payment Status

```bash
curl -X GET "http://localhost:8086/payment/pay-12345/status" \
  -H "Authorization: Bearer your_jwt_token"
```

### Process Refund

```bash
curl -X POST http://localhost:8086/payment/pay-12345/refund \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer your_jwt_token" \
  -d '{
    "amount": 5000,
    "reason": "Refund due to cancellation",
    "refundMethod": "ORIGINAL"
  }'
```

### Get Sales Report

```bash
curl -X GET "http://localhost:8086/payment/sales/daily?date=2024-01-15" \
  -H "Authorization: Bearer your_jwt_token"
```

## Payment Provider Integration

### Credit Card Payments

- Stripe API integration
- Square API integration
- PayPal API integration
- Domestic payment gateway services

### Electronic Money Payments

- Transit IC cards
- Rakuten Edy
- iD, QUICPay
- PayPay, LINE Pay

### QR Code Payments

- PayPay
- Rakuten Pay
- d-払い (d-Payment)
- au PAY

## Monitoring and Logging

### Health Check

- `/health` endpoint for service status check
- Database connection status
- Payment gateway connection status
- Redis/Kafka connection status

### Metrics

- Payment success rate
- Average processing time
- Refund rate
- Fraud detection rate

### Logs

- Payment processing logs
- Refund processing logs
- Fraud detection logs
- Error logs

## Security Considerations

### Recommended Settings for Production Environment

1. **Protection of Card Information**
   - PCI DSS compliance
   - Non-retention of card information

2. **Communication Security**
   - Use of TLS 1.3
   - Certificate pinning

3. **Fraud Prevention**
   - 3D Secure support
   - Risk-based authentication

## Troubleshooting

### Common Issues

1. **Payment Failure**
   - Check network connection
   - Check validity of API key

2. **Performance Issues**
   - Check payment gateway response time
   - Database connection pool settings

3. **Fraud Detection False Positives**
   - Adjust threshold settings
   - Whitelist settings

## Future Expansion Plans

- [ ] Cryptocurrency payment support
- [ ] Installment payment services
- [ ] Recurring payments
- [ ] API payments (Open Banking)
- [ ] Enhanced fraud detection with AI

## For Developers

### Code Structure

```text
src/main/java/
├── com/skiresort/payment/
│   ├── entity/          # JPA Entities
│   ├── service/         # Business Logic
│   ├── repository/      # Data Access
│   ├── resource/        # REST Endpoints
│   ├── gateway/         # Payment Gateway Integration
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
- Payment Gateway SDK

## License

This project is licensed under the MIT License.
````
