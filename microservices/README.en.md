# Ski Resort Management System - Microservice Architecture

## Overview

This is a comprehensive microservice-based ski resort management system built using Jakarta EE 11, Java 21 LTS, and WildFly 31.0.1. The system is designed to handle all aspects of ski resort operations, including user management, equipment rental, lift passes, lessons, payments, and customer support.

## Architecture

The system follows the microservice architecture pattern with the following components:

### 🚪 01. API Gateway Service (Port 8080)

- **Purpose**: Central entry point for all client requests
- **Functions**: Request routing, authentication, rate limiting, CORS handling
- **Technology**: Jakarta EE 11, JAX-RS, JWT, MicroProfile
- **Endpoints**: Routes to all downstream services based on path prefixes

### 👥 02. User Management Service (Port 8081)

- **Purpose**: User registration, profile management, authentication data
- **Functions**: User CRUD, profile management, role management, skill level tracking
- **Technology**: Jakarta EE 11, JPA/Hibernate, PostgreSQL
- **Entities**: User, UserProfile, UserPreferences

### 🎿 03. Product Catalog Service (Port 8082)

- **Purpose**: Equipment catalog, lift passes, lesson packages
- **Functions**: Product management, pricing, inventory status, categories
- **Technology**: Jakarta EE 11, JPA/Hibernate, PostgreSQL
- **Entities**: Product, Category, PriceRule, ProductVariant

### 🔐 04. Authentication Service (Port 8083)

- **Purpose**: User authentication, JWT token management
- **Functions**: Login/logout, token refresh, session management
- **Technology**: Jakarta EE 11, JWT, BCrypt, Redis for session storage
- **Functions**: Multi-factor authentication, password reset, account confirmation

### 📦 05. Inventory Management Service (Port 8084)

- **Purpose**: Equipment inventory, availability tracking
- **Functions**: Inventory management, reservations, maintenance tracking
- **Technology**: Jakarta EE 11, JPA/Hibernate, PostgreSQL
- **Entities**: Equipment, InventoryItem, Reservation, MaintenanceRecord

### 🛒 06. Order Management Service (Port 8085)

- **Purpose**: Order processing, reservation management
- **Functions**: Order lifecycle, reservation confirmation, cancellation
- **Technology**: Jakarta EE 11, JPA/Hibernate, Event Sourcing
- **Entities**: Order, OrderItem, Booking, Reservation

### 💳 07. Payment Service (Port 8086)

- **Purpose**: Payment processing, transaction management
- **Functions**: Multiple payment methods, refunds, payment tracking
- **Technology**: Jakarta EE 11, Payment gateway integration, PCI compliance
- **Entities**: Payment, Transaction, PaymentMethod, Refund

### 🛍️ 08. Shopping Cart Service (Port 8087)

- **Purpose**: Shopping cart management, session handling
- **Functions**: Cart persistence, item management, price calculation
- **Technology**: Jakarta EE 11, Redis for cart storage, real-time updates
- **Functions**: Cart sharing, saved carts, automatic cleanup

### 🎫 09. Coupon & Discount Service (Port 8088)

- **Purpose**: Promotion codes, discount management
- **Functions**: Coupon validation, discount calculation, usage tracking
- **Technology**: Jakarta EE 11, JPA/Hibernate, PostgreSQL
- **Entities**: Coupon, Discount, PromotionRule, UsageRecord

### ⭐ 10. Points & Loyalty Service (Port 8089)

- **Purpose**: Customer loyalty program, points management
- **Functions**: Point accrual, redemption, tier management
- **Technology**: Jakarta EE 11, JPA/Hibernate, PostgreSQL
- **Entities**: LoyaltyAccount, PointsTransaction, LoyaltyTier

### 🤖 11. AI Support Service (Port 8090)

- **Purpose**: Intelligent customer support, chatbot
- **Functions**: Natural language processing, automated responses, escalation
- **Technology**: Jakarta EE 11, AI/ML integration, WebSocket for real-time chat
- **Functions**: Multilingual support, sentiment analysis

### 🖥️ 12. Frontend Service (Port 8091)

- **Purpose**: Web frontend, customer and admin interfaces
- **Functions**: Responsive UI, real-time updates, PWA features
- **Technology**: Modern web technologies, WebSocket, Service Worker
- **Interfaces**: Customer portal, admin dashboard, staff interface

## Technology Stack

### Core Technologies

- **Java 21 LTS** - Virtual threads and modern language features
- **Jakarta EE 11** - Enterprise features
- **WildFly 31.0.1** - Application server
- **MicroProfile 6.1** - Microservice specifications

### Database & Persistence

- **PostgreSQL** - Primary data storage
- **Hibernate 6.4.1** - ORM
- **Redis** - Cache and session storage
- **H2** - For testing

### Security

- **JWT** - Authentication tokens
- **BCrypt** - Password hashing
- **HTTPS/TLS** - Secure communication
- **OAuth2/OpenID Connect** - External authentication

### Communication

- **JAX-RS** - REST APIs
- **WebSocket** - Real-time communication
- **JMS** - Asynchronous messaging
- **gRPC** - High-performance inter-service communication

### Monitoring & Observability

- **MicroProfile Health** - Health checks
- **MicroProfile Metrics** - Application metrics
- **MicroProfile OpenTracing** - Distributed tracing
- **Structured Logging** - JSON format

## Getting Started

### Prerequisites

- Java 21 LTS
- Maven 3.9+
- PostgreSQL 15+
- Redis 7+
- WildFly 31.0.1

### Building the Project

```bash
# Clone the repository
git clone https://github.com/jakartaone2025/ski-resort-system.git
cd ski-resort-system/microservices

# Build all services
mvn clean compile

# Run tests
mvn test

# Package all services
mvn package
```

### Running the Services

#### Option 1: Individual Services

```bash
# API Gateway
cd 01-api-gateway-service
mvn wildfly:deploy

# User Management
cd 02-user-management-service
mvn wildfly:deploy
```

#### Option 2: Docker Compose

```bash
# Start all services including dependencies
docker-compose up -d
```

### Service URLs

- API Gateway: <http://localhost:8080>
- User Management: <http://localhost:8081>
- Product Catalog: <http://localhost:8083>
- Authentication: <http://localhost:8084>
- Inventory Management: <http://localhost:8085>
- Order Management: <http://localhost:8086>
- Payment Service: <http://localhost:8087>
- Shopping Cart: <http://localhost:8088>
- Coupon & Discount: <http://localhost:8089>
- Points & Loyalty: <http://localhost:8090>
- AI Support: <http://localhost:8091>
- Frontend: <http://localhost:8092>

## API Documentation

Each service provides OpenAPI documentation at:

- `http://localhost:<port>/<service-name>/api/openapi`

## Configuration

Configuration is managed with MicroProfile Config:

- **Environment Variables** - For production deployments
- **Property Files** - For development
- **Kubernetes ConfigMaps** - For containerized environments

## Database Schema

Each service manages its own database schema:

- **User Management**: User profiles, authentication data
- **Product Catalog**: Products, categories, pricing
- **Inventory**: Equipment, stock levels, reservations
- **Orders**: Order history, bookings, fulfillment
- **Payments**: Transactions, payment methods
- **Loyalty**: Points, tiers, rewards

## Security & Authentication

### Authentication Flow

1. User authenticates via the Authentication Service
2. A JWT token with user claims is issued
3. The API Gateway validates the token on all requests
4. Inter-service communication uses JWT forwarding

### Authorization

- **Role-Based Access Control (RBAC)**
- **Resource-level permissions**
- **API key authentication for service-to-service communication**

## Monitoring

### Health Checks

- `/health` - MicroProfile Health endpoint
- `/health/live` - Liveness probe
- `/health/ready` - Readiness probe

### Metrics

- `/metrics` - Prometheus-compatible metrics
- Custom business metrics for each service

### Logging

- Structured JSON logs
- Correlation IDs for request tracing
- Centralized log aggregation

## Development Guidelines

### Coding Standards

- Java 21 best practices
- Constructor injection over field injection
- Comprehensive error handling
- Unit and integration tests

### Testing Strategy

- **Unit Tests** - JUnit 5 and Mockito
- **Integration Tests** - TestContainers
- **Contract Tests** - Pact
- **E2E Tests** - REST Assured

### CI/CD Pipeline

- **Build**: Maven compile and test
- **Quality**: SonarQube code analysis
- **Security**: OWASP dependency scanning
- **Deploy**: Blue-green deployment strategy

## Scalability & Performance

### Horizontal Scaling

- Stateless service design
- Database connection pooling
- Load balancing with HAProxy/NGINX

### Caching Strategy

- **Redis** - For sessions and frequently accessed data
- **CDN** - For static content
- **Application-level** - Caching with Caffeine

### Performance Optimization

- **Java 21 Virtual Threads** - For improved concurrency
- **Connection Pooling** - For database efficiency
- **Asynchronous Processing** - For long-running operations

## Deployment

### Container Strategy

- **Docker** - Containers for each service
- **Multi-stage builds** - For optimized images
- **Health checks** - Integrated into containers

### Kubernetes

- **Helm charts** - For deployment templates
- **ConfigMaps** - For configuration management
- **Secrets** - For sensitive data
- **Ingress** - For external access

### Production Monitoring

- **Prometheus** - For metrics collection
- **Grafana** - For dashboards
- **ELK Stack** - For log analysis
- **Jaeger** - For distributed tracing

## How to Contribute

1. Fork the repository
2. Create a feature branch
3. Implement changes with tests
4. Submit a pull request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Support

For questions and support:

- **Documentation**: [Wiki](https://github.com/jakartaone2025/ski-resort-system/wiki)
- **Issues**: [GitHub Issues](https://github.com/jakartaone2025/ski-resort-system/issues)
- **Discussions**: [GitHub Discussions](https://github.com/jakartaone2025/ski-resort-system/discussions)
