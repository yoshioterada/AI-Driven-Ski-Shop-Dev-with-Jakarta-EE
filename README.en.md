# Ski Resort Management System - Microservice Architecture

# Demo Video

[▶️ Watch the Demo Video](https://flic.kr/p/2rjkFZi)

## 📖 Project Overview

This project is a comprehensive ski resort management system built using Jakarta EE 11, Java 21 LTS, and the latest microservice architecture. It provides all the necessary functions for ski resort operations, including user management, equipment rental, lift tickets, lessons, payments, and customer support.

## 🏗️ Overall System Architecture Diagram

```text
┌─────────────────────────────────────────────────────────────────┐
│                        Frontend Service                         │
│                      (Next.js - Port 3000)                     │
│                         ✅ Implemented                           │
└─────────────────────┬───────────────────────────────────────────┘
                      │
┌─────────────────────┴───────────────────────────────────────────┐
│                    API Gateway Service                          │
│                 (Jakarta EE - Port 8080)                       │
│                       🚧 In Progress                             │
└─────────────┬───┬───┬───┬───┬───┬───┬───┬───┬───┬───────────────┘
              │   │   │   │   │   │   │   │   │   │
     ┌────────┘   │   │   │   │   │   │   │   │   │
     │  ┌─────────┘   │   │   │   │   │   │   │   │
     │  │  ┌──────────┘   │   │   │   │   │   │   │
     │  │  │  ┌───────────┘   │   │   │   │   │   │
     │  │  │  │  ┌────────────┘   │   │   │   │   │
     │  │  │  │  │  ┌─────────────┘   │   │   │   │
     │  │  │  │  │  │  ┌──────────────┘   │   │   │
     │  │  │  │  │  │  │  ┌───────────────┘   │   │
     │  │  │  │  │  │  │  │  ┌────────────────┘   │
     │  │  │  │  │  │  │  │  │  ┌─────────────────┘
     ▼  ▼  ▼  ▼  ▼  ▼  ▼  ▼  ▼  ▼
   ┌─────┐ ┌─────┐ ┌─────┐ ┌─────┐ ┌─────┐
   │User │ │Auth │ │Prod │ │Inv. │ │Order│
   │Mgmt │ │     │ │Cat. │ │Mgmt │ │Mgmt │
   │8081 │ │8083 │ │8082 │ │8084 │ │8085 │
   │🚧   │ │🚧   │ │✅   │ │🚧   │ │🚧   │
   └─────┘ └─────┘ └─────┘ └─────┘ └─────┘

   ┌─────┐ ┌─────┐ ┌─────┐ ┌─────┐ ┌─────┐
   │Pay  │ │Cart │ │Coup │ │Loy. │ │AI   │
   │Svc  │ │Svc  │ │Svc  │ │Svc  │ │Sup. │
   │8086 │ │8087 │ │8088 │ │8089 │ │8091 │
   │🚧   │ │✅   │ │🚧   │ │🚧   │ │✅   │
   └─────┘ └─────┘ └─────┘ └─────┘ └─────┘

   ┌─────────────────────────────────────────┐
   │            Database Layer               │
   │  ┌─────────┐  ┌─────────┐  ┌─────────┐ │
   │  │PostgreSQL│  │  Redis  │  │   H2    │ │
   │  │(Production)│  │(Cache)  │  │(Test)   │ │
   │  └─────────┘  └─────────┘  └─────────┘ │
   └─────────────────────────────────────────┘
```

**Legend:**

- ✅ **Implemented**: Fully implemented and tested
- 🚧 **In Progress**: Basic implementation complete, integration in progress
- ❌ **Not Implemented**: Not yet implemented

## 🛠️ Technology Stack

### 📋 Programming Languages Used

- **Java 21 LTS** - Backend services (utilizing the latest features like virtual threads, pattern matching, etc.)
- **TypeScript** - Frontend service
- **SQL** - Database schema definition

### 🏢 Jakarta EE Framework

- **Jakarta EE 11** - Foundation for enterprise features
- **JAX-RS** - Implementation of REST APIs
- **JPA/Hibernate 6.4.1** - Object-Relational Mapping
- **CDI (Contexts and Dependency Injection)** - Dependency injection
- **Bean Validation** - Data validation
- **MicroProfile 6.1** - Microservice specifications
  - MicroProfile Health - Health checks
  - MicroProfile Metrics - Metrics collection
  - MicroProfile Config - Configuration management
  - MicroProfile OpenAPI - API documentation generation

### 🖥️ Frontend Technologies

- **Next.js 15** - React framework
- **React 19** - UI library
- **TypeScript** - Type safety
- **Tailwind CSS** - Styling
- **Zustand** - State management

### 🗄️ Database & Storage

- **PostgreSQL 15+** - Main database
- **Redis 7+** - Cache and session management
- **H2 Database** - Test environment

### 🔐 Security & Authentication

- **JWT (JSON Web Tokens)** - Authentication tokens
- **BCrypt** - Password hashing
- **HTTPS/TLS** - Communication encryption

### 🌐 External Services & Libraries

- **Azure OpenAI GPT-4o** - AI support service
- **LangChain4j 1.1.0** - AI chain processing
- **WildFly 31.0.1** - Application server (for Jakarta EE services)
- **Docker** - Containerization
- **Maven 3.9+** - Build tool

### 📊 Monitoring & Operations

- **Prometheus** - Metrics collection
- **Grafana** - Dashboards
- **Structured Logging (JSON)** - Log management

## 🏛️ Microservice Details

### ✅ **Implemented Services**

#### 🖥️ 12. Frontend Service (Port 3000)

- **Purpose**: Web interface for users
- **Implementation Status**: ✅ **Fully Implemented**
- **Key Features**:
  - Responsive design
  - User authentication UI
  - Product catalog display
  - Shopping cart functionality
  - AI support chat
  - Developer panel (dummy user function)
- **Technology**: Next.js 15, React 19, TypeScript, Tailwind CSS
- **Characteristics**: PWA compatible, real-time updates, multilingual support

#### 🎿 03. Product Catalog Service (Port 8082)

- **Purpose**: Product and service catalog management
- **Implementation Status**: ✅ **Fully Implemented**
- **Key Features**:
  - Product CRUD operations
  - Category management
  - Pricing and management
  - Inventory status display
  - OpenAPI documentation generation
- **Technology**: Jakarta EE 11, JPA/Hibernate, PostgreSQL
- **Entities**: Product, Category, PriceRule, ProductVariant

#### 🛍️ 08. Shopping Cart Service (Port 8087)

- **Purpose**: Shopping cart management
- **Implementation Status**: ✅ **Fully Implemented**
- **Key Features**:
  - Cart persistence
  - Add/remove/quantity change of products
  - Price calculation
  - Session management
  - Real-time updates
- **Technology**: Quarkus 3.15.1, Redis, WebSocket
- **Characteristics**: Fast response, automatic cleanup

#### 🤖 11. AI Support Service (Port 8091)

- **Purpose**: AI-driven customer support
- **Implementation Status**: ✅ **Fully Implemented**
- **Key Features**:
  - Natural language processing
  - Automatic response generation
  - Multilingual support
  - Escalation function
  - Real-time chat
- **Technology**: Quarkus 3.15.1, Java 21, LangChain4j, Azure OpenAI GPT-4o
- **Characteristics**: Utilizes Java 21 preview features, pattern matching

### 🚧 **Services in Progress** (Basic implementation complete, integration in progress)

#### 🚪 01. API Gateway Service (Port 8080)

- **Purpose**: Unified entry point for all services
- **Functions**: Request routing, authentication, rate limiting, CORS processing

#### 👥 02. User Management Service (Port 8081)

- **Purpose**: User registration and profile management
- **Functions**: User CRUD, profile management, role management

#### 🔐 04. Authentication Service (Port 8083)

- **Purpose**: User authentication and JWT management
- **Functions**: Login/logout, token refresh, session management

#### 📦 05. Inventory Management Service (Port 8084)

- **Purpose**: Equipment inventory and availability tracking
- **Functions**: Inventory management, reservations, maintenance tracking

#### 🛒 06. Order Management Service (Port 8085)

- **Purpose**: Order processing and reservation management
- **Functions**: Order lifecycle, reservation confirmation, cancellation

#### 💳 07. Payment Service (Port 8086)

- **Purpose**: Payment processing and transaction management
- **Functions**: Multiple payment methods, refunds, payment tracking

#### 🎫 09. Coupon & Discount Service (Port 8088)

- **Purpose**: Promotion code and discount management
- **Functions**: Coupon validation, discount calculation, usage tracking

#### ⭐ 10. Points & Loyalty Service (Port 8089)

- **Purpose**: Customer loyalty program
- **Functions**: Point acquisition, redemption, tier management

### ⚠️ **Current Status of Inter-Service Collaboration**

**Implemented Collaborations**:

- Frontend ↔ Product Catalog ✅
- Frontend ↔ Shopping Cart ✅
- Frontend ↔ AI Support ✅
- Shopping Cart ↔ Product Catalog ✅

**Unimplemented Collaborations**:

- Collaboration between other services is currently in progress
- Integrated access via API Gateway is in progress
- Integration with the authentication service is in progress

## 🚀 Service Startup Procedure

### 📋 Prerequisites

- Java 21 LTS
- Node.js 18+
- Maven 3.9+
- Docker & Docker Compose
- PostgreSQL 15+ (provided by Docker)
- Redis 7+ (provided by Docker)

### 🔄 Startup Order (Recommended)

#### 1️⃣ **Infrastructure Startup**

```bash
# Start PostgreSQL and Redis
cd /Users/teradayoshio/GitHub/JakartaOne2025/microservices
docker-compose up -d postgres redis
```

#### 2️⃣ **Product Catalog Service Startup**

```bash
cd 03-product-catalog-service

# Initialize database
docker-compose up -d postgres

# Start service
./run.sh
# or
mvn clean compile quarkus:dev

# Verify: http://localhost:8082/health
```

#### 3️⃣ **Shopping Cart Service Startup**

```bash
cd 08-shopping-cart-service

# Start service
./run.sh
# or
mvn clean compile quarkus:dev

# Verify: http://localhost:8087/health
```

#### 4️⃣ **AI Support Service Startup**

```bash
cd 11-ai-support-service

# Start in Docker environment
docker-compose up -d

# Verify: http://localhost:8091/health
```

#### 5️⃣ **Frontend Service Startup**

```bash
cd 12-frontend-service

# Install dependencies
npm install

# Start development server
npm run dev

# Access: http://localhost:3000
```

### 🔧 **Dummy User for Development**

A dummy user feature for development is implemented in the frontend:

- **Email**: `demo@skiresort.com`
- **Password**: `demo123`
- **Name**: Taro Tanaka
- **Permissions**: Customer permissions (product viewing, purchasing, etc.)

**How to use**:

1. Access <http://localhost:3000>
2. Enable the dummy user in the "Developer Panel" in the upper right corner of the screen
3. On the login page, click "Enter demo account information"
4. All features can be tested

### 📊 **Health Check & Monitoring**

Health check endpoints for each service:

- Product Catalog: <http://localhost:8082/health>
- Shopping Cart: <http://localhost:8087/health>
- AI Support: <http://localhost:8091/health>
- Frontend: <http://localhost:3000> (Next.js built-in health)

### 🔍 **API Documentation**

API documentation for implemented services:

- Product Catalog: <http://localhost:8082/q/openapi>
- Shopping Cart: <http://localhost:8087/q/swagger-ui>
- AI Support: <http://localhost:8091/q/swagger-ui>

## 🎯 Future Development Plans

### 📅 Short-Term Goals (1-2 weeks)

1. **Complete API Gateway Service** - Establish a unified entry point
2. **Integrate Authentication Service** - Full implementation of JWT-based authentication
3. **Integrate User Management Service** - User profile management

### 📅 Mid-Term Goals (1 month)

1. **Complete Order & Payment Flow** - End-to-end purchasing experience
2. **Integrate Inventory Management System** - Real-time inventory tracking
3. **Loyalty Program** - Point acquisition and usage system

### 📅 Long-Term Goals (2-3 months)

1. **Deploy to Production Environment** - Kubernetes cluster deployment
2. **Performance Optimization** - Utilize Java 21 virtual threads
3. **High Availability Support** - Fault recovery and scaling support

## 🤝 Information for Developers

### 📝 Coding Standards

- Actively utilize modern features of Java 21
- Recommend constructor injection for dependency injection
- Comprehensive error handling
- Mandatory unit and integration tests

### 🧪 Testing Strategy

- **Unit Tests**: JUnit 5 + Mockito
- **Integration Tests**: TestContainers
- **Contract Tests**: OpenAPI specification compliance
- **E2E Tests**: REST Assured

### 📈 Performance Design

- **Java 21 Virtual Threads** - High concurrency processing
- **Connection Pooling** - Database efficiency
- **Redis Cache** - High-speed data access
- **Asynchronous Processing** - Long-running tasks

## 📄 License

This project is released under the MIT License.

## 📞 Support & Inquiries

- **Project Wiki**: [GitHub Wiki](https://github.com/jakartaone2025/ski-resort-system/wiki)
- **Issues**: [GitHub Issues](https://github.com/jakartaone2025/ski-resort-system/issues)
- **Discussions**: [GitHub Discussions](https://github.com/jakartaone2025/ski-resort-system/discussions)

---

**Last Updated**: July 28, 2025  
**Project Status**: Active Development 🚧
