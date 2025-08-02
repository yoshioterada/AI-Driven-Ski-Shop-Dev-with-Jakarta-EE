# Ski Equipment Sales Shop Site Microservice Design Document

## Table of Contents

1. [Overview](#overview)
2. [System Architecture](#system-architecture)
3. [Microservice Specifications](#microservice-specifications)
4. [Data Architecture](#data-architecture)
5. [Security Design](#security-design)
6. [API Design](#api-design)
7. [Infrastructure Design](#infrastructure-design)
8. [Handling Non-Functional Requirements](#handling-non-functional-requirements)
9. [Infrastructure](#infrastructure)
10. [Operation and Monitoring](#operation-and-monitoring)
11. [Development and Operation Process](#development-and-operation-process)
12. [Risk Management](#risk-management)
13. [Development and Deployment](#development-and-deployment)

## Overview

### System Overview

We will build an online shop site for selling ski equipment with a microservice architecture based on **Jakarta EE 11** and **Java 21 LTS**. We will utilize the latest Java features such as Virtual Threads, Record classes, and Pattern Matching to achieve high concurrency and maintainability.

### Business Requirements

- **Target Customers**: Ski enthusiasts, from beginners to advanced skiers
- **Product Categories**: Skis, boots, apparel, accessories, maintenance supplies
- **Expected Concurrent Users**: 1,000 to 10,000 (at peak times)
- **Sales Target**: 5 billion yen annually
- **Region**: Nationwide in Japan (with preparation for multilingual support)

### Technical Requirements

- **Base Technology**: Jakarta EE 11, Java 21 LTS
- **Architecture**: Microservices
- **Cloud**: Microsoft Azure
- **Containers**: Docker + Kubernetes
- **Databases**: PostgreSQL, Redis, MongoDB
- **Messaging**: Apache Kafka, Azure Service Bus

## System Architecture

### Overall Architecture Diagram

```mermaid
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   Web Frontend  │    │   Mobile Apps    │    │  Admin Portal   │
│     (React)     │    │ (React Native)   │    │    (Angular)    │
└─────────────────┘    └──────────────────┘    └─────────────────┘
          │                       │                       │
          └───────────────────────┼───────────────────────┘
                                  │
                    ┌─────────────────────────┐
                    │     API Gateway         │
                    │   (Kong / Azure APIM)   │
                    └─────────────────────────┘
                                  │
          ┌───────────────────────┼───────────────────────┐
          │                       │                       │
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│ Authentication  │    │ User Management │    │ Product Catalog │
│    Service      │    │    Service      │    │    Service      │
└─────────────────┘    └─────────────────┘    └─────────────────┘
          │                       │                       │
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│ Inventory Mgmt  │    │ Order/Sales     │    │ Payment/Cart    │
│    Service      │    │    Service      │    │    Service      │
└─────────────────┘    └─────────────────┘    └─────────────────┘
          │                       │                       │
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│ Coupon/Discount │    │ Points/Loyalty  │    │ AI Support      │
│    Service      │    │    Service      │    │    Service      │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

### Microservice Configuration

| Service Name | Responsibility | Technology Stack |
|-----------|------|-------------|
| API Gateway | Routing, authentication, rate limiting | Kong, Azure API Management |
| Authentication Service | OAuth 2.0/OIDC authentication | Jakarta Security, Keycloak |
| User Management Service | User profile management | Jakarta EE 11, PostgreSQL |
| Product Catalog Service | Product information management | Jakarta EE 11, PostgreSQL |
| Inventory Management Service | Inventory management | Jakarta EE 11, PostgreSQL |
| Order/Sales Service | Order and sales processing | Jakarta EE 11, PostgreSQL |
| Payment/Cart Service | Payment and cart processing | Jakarta EE 11, PostgreSQL |
| Coupon/Discount Service | Coupon and discount management | Jakarta EE 11, Redis |
| Points/Loyalty Service | Points and loyalty | Jakarta EE 11, PostgreSQL |
| AI Support Service | AI chatbot | Jakarta EE 11, Azure OpenAI |
| Frontend Service | Website delivery | React, Next.js |

## Microservice Specifications

### 1. API Gateway Service

**Responsibility**: Unified entry point for all external requests

**Key Features**:

- Request routing
- Centralized authentication and authorization
- Rate limiting and throttling
- Request/response transformation
- Load balancing
- Monitoring and log collection

**Technology Stack**:

- Kong Gateway / Azure API Management
- OAuth 2.0 / OpenID Connect
- Rate Limiting (Redis)
- Health Check Integration

**API Endpoints**:

```yaml
/api/v1/auth/*          → Authentication Service
/api/v1/users/*         → User Management Service
/api/v1/products/*      → Product Catalog Service
/api/v1/inventory/*     → Inventory Management Service
/api/v1/orders/*        → Order/Sales Service
/api/v1/payments/*      → Payment/Cart Service
/api/v1/coupons/*       → Coupon/Discount Service
/api/v1/points/*        → Points/Loyalty Service
/api/v1/support/*       → AI Support Service
```

### 2. Authentication Service

**Responsibility**: Authentication and authorization via OAuth 2.0/OpenID Connect

**Key Features**:

- OAuth 2.0 Authorization Server
- JWT token issuance and validation
- Refresh token management
- Social login integration (Google, Facebook, Line)
- MFA (Multi-Factor Authentication)
- Session management

**Technology Stack**:

- Jakarta Security 3.1
- Keycloak (Identity Provider)
- Jakarta REST 4.0
- PostgreSQL (user authentication information)
- Redis (session management)

**Main API**:

```java
// Jakarta EE Record-based API
@Path("/auth")
@ApplicationScoped
public class AuthenticationResource {
    
    // Start OAuth 2.0 authentication
    @POST
    @Path("/oauth/authorize")
    public Response authorize(AuthorizeRequest request);
    
    // Get access token
    @POST
    @Path("/oauth/token")
    public TokenResponse getToken(TokenRequest request);
    
    // Validate token
    @POST
    @Path("/oauth/verify")
    public TokenValidationResponse validateToken(String token);
    
    // Logout
    @POST
    @Path("/logout")
    public Response logout(@HeaderParam("Authorization") String token);
}

// Record class-based data transfer
public record AuthorizeRequest(
    String clientId,
    String redirectUri,
    String scope,
    String state
) {}

public record TokenResponse(
    String accessToken,
    String refreshToken,
    String tokenType,
    int expiresIn,
    String scope
) {}
```

### 3. User Management Service

**Responsibility**: User profile and account information management

**Key Features**:

- User registration and profile management
- Personal information management (GDPR compliant)
- Shipping address management
- Favorite product management
- Order history reference
- Account settings

**Technology Stack**:

- Jakarta EE 11 (CDI 4.1, Jakarta Persistence 3.2)
- Jakarta Data 1.0 (Repository Pattern)
- PostgreSQL
- Virtual Threads (asynchronous processing)

**Domain Model**:

```java
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(unique = true)
    private String email;
    
    private String firstName;
    private String lastName;
    private LocalDate birthDate;
    private String phoneNumber;
    
    @Enumerated(EnumType.STRING)
    private UserStatus status;
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Address> addresses;
    
    @OneToMany(mappedBy = "user")
    private List<UserPreference> preferences;
    
    // Asynchronous method with Virtual Threads support
    @Asynchronous
    public CompletableFuture<List<Order>> getOrderHistoryAsync() {
        return CompletableFuture.supplyAsync(() -> {
            // Get order history process
            return orderService.findByUserId(this.id);
        });
    }
}

// Record-based Value Object
public record Address(
    String street,
    String city,
    String prefecture,
    String postalCode,
    String country,
    AddressType type
) {}

public enum AddressType {
    BILLING, SHIPPING, BOTH
}
```

**Main API**:

```java
@Path("/users")
@ApplicationScoped
public class UserResource {
    
    @Inject
    private UserRepository userRepository;
    
    @GET
    @Path("/{userId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUser(@PathParam("userId") UUID userId) {
        return userRepository.findById(userId)
            .map(user -> Response.ok(UserDTO.from(user)).build())
            .orElse(Response.status(404).build());
    }
    
    @PUT
    @Path("/{userId}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateUser(@PathParam("userId") UUID userId, 
                              UserUpdateRequest request) {
        // Implement business logic
    }
    
    @POST
    @Path("/{userId}/addresses")
    public Response addAddress(@PathParam("userId") UUID userId,
                              AddAddressRequest request) {
        // Add address process
    }
}
```

### 4. Product Catalog Service

**Responsibility**: Product information and catalog management

**Key Features**:

- Product master management
- Category management (hierarchical structure)
- Product search and filtering
- Product reviews and ratings
- Product image management
- Price management and sale prices
- Integration with product recommendation engine

**Technology Stack**:

- Jakarta EE 11
- PostgreSQL (product master)
- Elasticsearch (full-text search)
- Azure Blob Storage (image storage)
- Redis (cache)

**Domain Model**:

```java
@Entity
@Table(name = "products")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    private String name;
    private String description;
    private String brand;
    
    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;
    
    @Embedded
    private Price price;
    
    @ElementCollection
    @CollectionTable(name = "product_images")
    private List<ProductImage> images;
    
    @ElementCollection
    @Enumerated(EnumType.STRING)
    private Set<SkiType> skiTypes;
    
    // Product specification (Record class)
    @Embedded
    private SkiSpecification specification;
    
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

// Record-based Value Object
public record Price(
    BigDecimal regularPrice,
    BigDecimal salePrice,
    String currency,
    LocalDateTime saleStartDate,
    LocalDateTime saleEndDate
) {
    public BigDecimal getCurrentPrice() {
        LocalDateTime now = LocalDateTime.now();
        if (salePrice != null && 
            saleStartDate != null && saleEndDate != null &&
            now.isAfter(saleStartDate) && now.isBefore(saleEndDate)) {
            return salePrice;
        }
        return regularPrice;
    }
}

public record SkiSpecification(
    Integer length,
    String material,
    String flexRating,
    String terrainType,
    String skillLevel
) {}

public enum SkiType {
    ALL_MOUNTAIN, CARVING, FREESTYLE, RACING, BACKCOUNTRY
}
```

**Main API**:

```java
@Path("/products")
@ApplicationScoped
public class ProductResource {
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchProducts(
            @QueryParam("q") String query,
            @QueryParam("category") String category,
            @QueryParam("brand") String brand,
            @QueryParam("minPrice") BigDecimal minPrice,
            @QueryParam("maxPrice") BigDecimal maxPrice,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {
        
        var searchCriteria = new ProductSearchCriteria(
            query, category, brand, minPrice, maxPrice
        );
        
        var pageable = Pageable.of(page, size);
        var results = productSearchService.search(searchCriteria, pageable);
        
        return Response.ok(results).build();
    }
    
    @GET
    @Path("/{productId}")
    public Response getProduct(@PathParam("productId") UUID productId) {
        // Get product details
    }
    
    @GET
    @Path("/{productId}/recommendations")
    public Response getRecommendations(@PathParam("productId") UUID productId) {
        // Get AI recommended products
    }
}
```

### 5. Inventory Management Service

**Responsibility**: Inventory management and adjustment

**Key Features**:

- Real-time inventory management
- Inventory reservation and release
- Inbound and outbound management
- Inventory alerts and automatic ordering
- Location management (by warehouse)
- Inventory reports

**Technology Stack**:

- Jakarta EE 11
- PostgreSQL
- Redis (inventory cache)
- Apache Kafka (inventory events)

**Domain Model**:

```java
@Entity
@Table(name = "inventory")
public class Inventory {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "product_id")
    private UUID productId;
    
    @Column(name = "warehouse_id")
    private UUID warehouseId;
    
    private Integer availableQuantity;
    private Integer reservedQuantity;
    private Integer totalQuantity;
    
    private Integer reorderPoint;
    private Integer maxStock;
    
    private LocalDateTime lastUpdated;
    
    // Asynchronous inventory update with Virtual Threads
    @Asynchronous
    public CompletableFuture<Void> updateInventoryAsync(
            InventoryUpdateEvent event) {
        return CompletableFuture.runAsync(() -> {
            // Inventory update process
            applyInventoryChange(event);
            publishInventoryUpdateEvent();
        });
    }
}

// Record-based event
public record InventoryUpdateEvent(
    UUID productId,
    UUID warehouseId,
    Integer quantityChange,
    InventoryOperation operation,
    String reason,
    LocalDateTime timestamp
) {}

public enum InventoryOperation {
    RESERVE, RELEASE, RESTOCK, ADJUSTMENT, SALE
}
```

**Main API**:

```java
@Path("/inventory")
@ApplicationScoped
public class InventoryResource {
    
    @GET
    @Path("/products/{productId}")
    public Response getInventory(@PathParam("productId") UUID productId) {
        var inventory = inventoryService.getAvailableInventory(productId);
        return Response.ok(inventory).build();
    }
    
    @POST
    @Path("/reserve")
    public Response reserveInventory(InventoryReservationRequest request) {
        try {
            var reservation = inventoryService.reserveInventory(
                request.productId(),
                request.quantity(),
                request.customerId()
            );
            return Response.ok(reservation).build();
        } catch (InsufficientInventoryException e) {
            return Response.status(409)
                .entity(new ErrorResponse("INSUFFICIENT_INVENTORY", e.getMessage()))
                .build();
        }
    }
    
    @POST
    @Path("/release")
    public Response releaseInventory(InventoryReleaseRequest request) {
        // Inventory release process
    }
}
```

### 6. Order/Sales Service

**Responsibility**: Order processing and sales management

**Key Features**:

- Order processing workflow
- Order status management
- Integration with shipping management
- Order cancellation and return processing
- Sales reports
- Order history management

**Technology Stack**:

- Jakarta EE 11
- PostgreSQL
- Apache Kafka (order events)
- Saga Pattern (distributed transactions)

**Domain Model**:

```java
@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "customer_id")
    private UUID customerId;
    
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderItem> items;
    
    @Embedded
    private OrderAmount amount;
    
    @Embedded
    private ShippingAddress shippingAddress;
    
    @Enumerated(EnumType.STRING)
    private OrderStatus status;
    
    @Embedded
    private OrderTimestamps timestamps;
    
    // Distributed transaction with Saga Pattern
    @Transactional
    public OrderProcessResult processOrder() {
        // 1. Reserve inventory
        // 2. Process payment
        // 3. Arrange shipping
        // 4. Confirm order
        return new OrderProcessResult(this.id, OrderStatus.CONFIRMED);
    }
}

public record OrderAmount(
    BigDecimal subtotal,
    BigDecimal tax,
    BigDecimal shipping,
    BigDecimal discount,
    BigDecimal total,
    String currency
) {}

public record OrderTimestamps(
    LocalDateTime orderedAt,
    LocalDateTime confirmedAt,
    LocalDateTime shippedAt,
    LocalDateTime deliveredAt
) {}

public enum OrderStatus {
    PENDING, CONFIRMED, PAID, SHIPPED, DELIVERED, CANCELLED, RETURNED
}
```

### 7. Payment/Cart Service

**Responsibility**: Shopping cart and payment processing

**Key Features**:

- Shopping cart management
- Payment processing (credit card, electronic money)
- Payment fee calculation
- Installment and deferred payment support
- Payment status management
- Refund processing

**Technology Stack**:

- Jakarta EE 11
- PostgreSQL
- Redis (cart session)
- Stripe/PayPal (payment gateway)

**Domain Model**:

```java
@Entity
@Table(name = "shopping_carts")
public class ShoppingCart {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "customer_id")
    private UUID customerId;
    
    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL)
    private List<CartItem> items;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime expiresAt;
    
    // Asynchronous price calculation with Virtual Threads
    @Asynchronous
    public CompletableFuture<CartTotal> calculateTotalAsync() {
        return CompletableFuture.supplyAsync(() -> {
            return items.stream()
                .map(item -> item.calculateSubtotal())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        }).thenCompose(subtotal -> 
            taxService.calculateTaxAsync(subtotal)
                .thenApply(tax -> new CartTotal(subtotal, tax, subtotal.add(tax)))
        );
    }
}

public record CartTotal(
    BigDecimal subtotal,
    BigDecimal tax,
    BigDecimal total
) {}

@Entity
@Table(name = "payments")
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "order_id")
    private UUID orderId;
    
    @Embedded
    private PaymentAmount amount;
    
    @Enumerated(EnumType.STRING)
    private PaymentMethod method;
    
    @Enumerated(EnumType.STRING)
    private PaymentStatus status;
    
    private String transactionId;
    private String gatewayResponse;
    
    private LocalDateTime processedAt;
}

public enum PaymentMethod {
    CREDIT_CARD, DEBIT_CARD, PAYPAL, APPLE_PAY, GOOGLE_PAY, BANK_TRANSFER
}

public enum PaymentStatus {
    PENDING, PROCESSING, COMPLETED, FAILED, REFUNDED, CANCELLED
}
```

### 8. Coupon/Discount Service

**Responsibility**: Coupon and discount management

**Key Features**:

- Coupon issuance and management
- Discount rule settings
- Promotion management
- Coupon application validation
- Usage history management
- Limited-time sales

**Technology Stack**:

- Jakarta EE 11
- Redis (for fast access)
- PostgreSQL (for persistence)

**Domain Model**:

```java
@Entity
@Table(name = "coupons")
public class Coupon {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(unique = true)
    private String code;
    
    private String name;
    private String description;
    
    @Embedded
    private DiscountRule discountRule;
    
    @Embedded
    private CouponValidityPeriod validityPeriod;
    
    private Integer usageLimit;
    private Integer usedCount;
    private Boolean active;
    
    // Calculate discount with rule engine pattern
    public DiscountResult calculateDiscount(OrderAmount orderAmount) {
        return discountRule.apply(orderAmount);
    }
}

public record DiscountRule(
    DiscountType type,
    BigDecimal value,
    BigDecimal minimumOrderAmount,
    Set<UUID> applicableProductIds,
    Set<String> applicableCategories
) {
    public DiscountResult apply(OrderAmount orderAmount) {
        // Discount calculation logic
        return switch (type) {
            case PERCENTAGE -> applyPercentageDiscount(orderAmount);
            case FIXED_AMOUNT -> applyFixedAmountDiscount(orderAmount);
            case FREE_SHIPPING -> applyFreeShippingDiscount(orderAmount);
        };
    }
}

public enum DiscountType {
    PERCENTAGE, FIXED_AMOUNT, FREE_SHIPPING
}
```

### 9. Points/Loyalty Service

**Responsibility**: Points and loyalty program management

**Key Features**:

- Point issuance and consumption
- Loyalty level management
- Point expiration management
- Bonus point campaigns
- Point history management
- Member rank benefits

**Technology Stack**:

- Jakarta EE 11
- PostgreSQL
- Redis (point balance cache)

### 10. AI Support Service

**Responsibility**: AI-powered customer support

**Key Features**:

- Chatbot (Azure OpenAI GPT-4)
- Product recommendation engine
- FAQ auto-response
- Inquiry classification and routing
- Sentiment analysis
- Multilingual support

**Technology Stack**:

- Jakarta EE 11
- Azure OpenAI Service
- MongoDB (conversation history)
- Azure Cognitive Services

### 11. Frontend Service

**Responsibility**: Website and SPA delivery

**Key Features**:

- Responsive web design
- PWA (Progressive Web App)
- SEO optimization
- Performance optimization
- A/B testing functionality

**Technology Stack**:

- React 18
- Next.js 14
- TypeScript
- Azure Static Web Apps

## Data Architecture

### Database Design

**PostgreSQL Cluster**:

- **Primary Database**: Users, products, orders, inventory
- **Read Replicas**: Reports, analytical queries
- **Sharding**: Large volume data (order history, logs)

**Redis Cluster**:

- **Session Store**: User sessions
- **Cache**: Product information, inventory information
- **Queue**: Asynchronous processing tasks

**MongoDB**:

- **Document Store**: AI conversation history, log data
- **Time Series**: Metrics, monitoring data

### Data Consistency Strategy

**Saga Pattern**: Distributed transaction management

```java
@ApplicationScoped
public class OrderProcessingSaga {
    
    @Inject
    private InventoryService inventoryService;
    
    @Inject
    private PaymentService paymentService;
    
    @Inject
    private ShippingService shippingService;
    
    // Asynchronous Saga execution with Virtual Threads
    @Asynchronous
    public CompletableFuture<SagaResult> processOrderSaga(OrderCreatedEvent event) {
        return CompletableFuture
            .supplyAsync(() -> reserveInventory(event))
            .thenCompose(this::processPayment)
            .thenCompose(this::arrangeShipping)
            .thenCompose(this::confirmOrder)
            .exceptionally(this::handleSagaFailure);
    }
    
    private CompletableFuture<SagaStep> reserveInventory(OrderCreatedEvent event) {
        return inventoryService.reserveInventoryAsync(event.getOrderItems())
            .thenApply(result -> new SagaStep("INVENTORY_RESERVED", result))
            .exceptionally(ex -> new SagaStep("INVENTORY_FAILED", ex));
    }
}
```

## Security Design

### Authentication and Authorization

**OAuth 2.0 / OpenID Connect Flow**:

1. Client authentication (Client Credentials)
2. Authorization Code Flow
3. Refresh token rotation
4. PKCE (Proof Key for Code Exchange)

**MicroProfile JWT Structure**:

```json
{
  "header": {
    "alg": "RS256",
    "typ": "JWT",
    "kid": "auth-service-key"
  },
  "payload": {
    "iss": "https://ski-equipment-shop.com",
    "sub": "user-uuid",
    "aud": "ski-equipment-shop",
    "exp": 1640995200,
    "iat": 1640991600,
    "groups": ["customer", "premium"],
    "permissions": ["read:profile", "write:cart", "read:orders"],
    "preferred_username": "user_12345678",
    "token_type": "access"
  }
}
```

**MicroProfile JWT Standard**:

- Jakarta EE 11 Web Profile compliant
- `groups` claim: User roles (MicroProfile JWT standard)
- `permissions` claim: Custom permissions
- `preferred_username` claim: Display username
- RS256 signature algorithm used

### Security Measures

**OWASP Top 10 Countermeasures**:

- SQL injection prevention (Jakarta Persistence)
- XSS prevention (CSP, input sanitization)
- CSRF prevention (SameSite Cookie, CSRF token)
- Security header settings
- Input value validation (Jakarta Validation)

**Data Protection**:

- TLS 1.3 (all communication encrypted)
- AES-256 (sensitive data encryption)
- PII pseudonymization and masking
- GDPR compliance (right to data erasure, etc.)

## API Design

### RESTful API Design Principles

**Richardson Maturity Model Level 3 Compliance**:

- **Level 0**: Use of HTTP
- **Level 1**: Resource-oriented URL design
- **Level 2**: Proper use of HTTP verbs
- **Level 3**: HATEOAS (Hypermedia as the Engine of Application State)

**API Design Guidelines**:

```yaml
# OpenAPI 3.1 Specification Example
openapi: 3.1.0
info:
  title: Ski Shop API
  version: 1.0.0
  description: Integrated API for the ski equipment sales shop

servers:
  - url: https://api.ski-shop.com/v1
    description: Production server
  - url: https://staging-api.ski-shop.com/v1
    description: Staging server

paths:
  /products:
    get:
      summary: Get product list
      parameters:
        - name: category
          in: query
          schema:
            type: string
          example: "skis"
        - name: page
          in: query
          schema:
            type: integer
            default: 0
        - name: size
          in: query
          schema:
            type: integer
            default: 20
            maximum: 100
      responses:
        '200':
          description: Product list
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ProductListResponse'
          links:
            nextPage:
              operationRef: '#/paths/~1products/get'
              parameters:
                page: '$response.body#/pagination/nextPage'

  /products/{productId}:
    get:
      summary: Get product details
      parameters:
        - name: productId
          in: path
          required: true
          schema:
            type: string
            format: uuid
      responses:
        '200':
          description: Product details
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ProductDetailResponse'
          links:
            addToCart:
              operationRef: '#/paths/~1cart~1items/post'
              parameters:
                productId: '$response.body#/id'

components:
  schemas:
    ProductListResponse:
      type: object
      properties:
        products:
          type: array
          items:
            $ref: '#/components/schemas/Product'
        pagination:
          $ref: '#/components/schemas/Pagination'
        _links:
          $ref: '#/components/schemas/Links'
```

### API Versioning Strategy

**Semantic Versioning Adopted**:

```java
// Versioning implementation with Jakarta REST
@Path("/v1/products")
@ApplicationScoped
@ApiVersion("1.0")
public class ProductResourceV1 {
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get Product List V1")
    public Response getProducts(
            @Parameter(description = "Category filter") 
            @QueryParam("category") String category,
            @Parameter(description = "Page number") 
            @QueryParam("page") @DefaultValue("0") int page) {
        
        var products = productService.findProducts(category, page);
        var response = ProductListResponseV1.builder()
            .products(products)
            .addLink("self", buildSelfLink())
            .addLink("next", buildNextPageLink(page + 1))
            .build();
            
        return Response.ok(response).build();
    }
}

// API version compatibility management
@ApplicationScoped
public class ApiVersionCompatibilityService {
    
    public <T> T convertResponse(Object sourceResponse, 
                                String targetVersion, 
                                Class<T> targetType) {
        return switch (targetVersion) {
            case "1.0" -> convertToV1(sourceResponse, targetType);
            case "2.0" -> convertToV2(sourceResponse, targetType);
            default -> throw new UnsupportedApiVersionException(targetVersion);
        };
    }
}
```

### Standard Error Handling

**RFC 7807 Problem Details Compliant**:

```java
// Problem Details implementation
public record ApiProblem(
    String type,
    String title,
    int status,
    String detail,
    String instance,
    Map<String, Object> extensions
) {
    
    public static ApiProblem builder() {
        return new ApiProblemBuilder();
    }
    
    // Standard error response
    public static ApiProblem validationError(List<ValidationError> errors) {
        return ApiProblem.builder()
            .type("https://api.ski-shop.com/problems/validation-error")
            .title("Validation Failed")
            .status(400)
            .detail("Request data has validation errors")
            .extension("violations", errors)
            .build();
    }
    
    public static ApiProblem resourceNotFound(String resourceType, String id) {
        return ApiProblem.builder()
            .type("https://api.ski-shop.com/problems/resource-not-found")
            .title("Resource Not Found")
            .status(404)
            .detail(String.format("%s with id %s was not found", resourceType, id))
            .build();
    }
}

// Exception handler
@Provider
public class GlobalExceptionHandler implements ExceptionMapper<Exception> {
    
    @Override
    public Response toResponse(Exception exception) {
        return switch (exception) {
            case ValidationException ve -> 
                Response.status(400)
                    .entity(ApiProblem.validationError(ve.getViolations()))
                    .type("application/problem+json")
                    .build();
                    
            case EntityNotFoundException enfe ->
                Response.status(404)
                    .entity(ApiProblem.resourceNotFound(enfe.getEntityType(), enfe.getId()))
                    .type("application/problem+json")
                    .build();
                    
            default ->
                Response.status(500)
                    .entity(ApiProblem.internalServerError())
                    .type("application/problem+json")
                    .build();
        };
    }
}
```

### Rate Limiting and Throttling

```java
// Rate limiting implementation with Jakarta CDI
@Interceptor
@RateLimit
@Priority(Interceptor.Priority.APPLICATION)
public class RateLimitInterceptor {
    
    @Inject
    private RateLimitService rateLimitService;
    
    @AroundInvoke
    public Object checkRateLimit(InvocationContext context) throws Exception {
        var method = context.getMethod();
        var rateLimitAnnotation = method.getAnnotation(RateLimit.class);
        
        var clientId = extractClientId(context);
        var key = generateRateLimitKey(clientId, method);
        
        if (!rateLimitService.isAllowed(key, 
                rateLimitAnnotation.requests(), 
                rateLimitAnnotation.period())) {
            throw new RateLimitExceededException();
        }
        
        return context.proceed();
    }
}

// Usage example
@Path("/products")
@ApplicationScoped
public class ProductResource {
    
    @GET
    @RateLimit(requests = 100, period = "1m")
    public Response getProducts() {
        // Implementation
    }
    
    @POST
    @RateLimit(requests = 10, period = "1m")
    @Authenticated
    public Response createProduct() {
        // Implementation
    }
}
```

## Infrastructure Design

### Local Development Environment (Docker Compose)

**Development Environment Configuration**:

```yaml
# docker-compose.yml
version: '3.9'

services:
  # API Gateway
  kong:
    image: kong:3.7
    environment:
      KONG_DATABASE: postgres
      KONG_PG_HOST: kong-database
      KONG_PG_PASSWORD: kong-password
      KONG_PROXY_ACCESS_LOG: /dev/stdout
      KONG_ADMIN_ACCESS_LOG: /dev/stdout
      KONG_PROXY_ERROR_LOG: /dev/stderr
      KONG_ADMIN_ERROR_LOG: /dev/stderr
      KONG_ADMIN_LISTEN: 0.0.0.0:8001
    ports:
      - "8000:8000"
      - "8001:8001"
    depends_on:
      - kong-database

  kong-database:
    image: postgres:16
    environment:
      POSTGRES_DB: kong
      POSTGRES_USER: kong
      POSTGRES_PASSWORD: kong-password
    volumes:
      - kong_data:/var/lib/postgresql/data

  # Microservices
  user-service:
    build:
      context: ./user-management-service
      dockerfile: Dockerfile.dev
    environment:
      - DATASOURCE_URL=jdbc:postgresql://postgres:5432/ski_shop_users
      - DATASOURCE_USERNAME=ski_shop_user
      - DATASOURCE_PASSWORD=password
      - REDIS_URL=redis://redis:6379
      - KAFKA_BOOTSTRAP_SERVERS=kafka:9092
    ports:
      - "8080:8080"
    depends_on:
      - postgres
      - redis
      - kafka
    volumes:
      - ./user-management-service/src:/app/src
      - ./user-management-service/target:/app/target

  product-service:
    build:
      context: ./product-catalog-service
      dockerfile: Dockerfile.dev
    environment:
      - DATASOURCE_URL=jdbc:postgresql://postgres:5432/ski_shop_products
      - ELASTICSEARCH_URL=http://elasticsearch:9200
      - REDIS_URL=redis://redis:6379
    ports:
      - "8081:8080"
    depends_on:
      - postgres
      - elasticsearch
      - redis

  # Databases
  postgres:
    image: postgres:16
    environment:
      POSTGRES_DB: ski_shop
      POSTGRES_USER: ski_shop_user
      POSTGRES_PASSWORD: password
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./scripts/init-databases.sql:/docker-entrypoint-initdb.d/init-databases.sql

  # Redis
  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data

  # Elasticsearch
  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:8.11.0
    environment:
      - discovery.type=single-node
      - xpack.security.enabled=false
      - "ES_JAVA_OPTS=-Xms512m -Xmx512m"
    ports:
      - "9200:9200"
    volumes:
      - elasticsearch_data:/usr/share/elasticsearch/data

  # Apache Kafka
  zookeeper:
    image: confluentinc/cp-zookeeper:7.5.0
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_TICK_TIME: 2000

  kafka:
    image: confluentinc/cp-kafka:7.5.0
    depends_on:
      - zookeeper
    ports:
      - "9092:9092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1

  # Monitoring
  prometheus:
    image: prom/prometheus:v2.47.0
    ports:
      - "9090:9090"
    volumes:
      - ./monitoring/prometheus.yml:/etc/prometheus/prometheus.yml
      - prometheus_data:/prometheus

  grafana:
    image: grafana/grafana:10.2.0
    ports:
      - "3000:3000"
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=admin
    volumes:
      - grafana_data:/var/lib/grafana
      - ./monitoring/grafana/dashboards:/etc/grafana/provisioning/dashboards

volumes:
  kong_data:
  postgres_data:
  redis_data:
  elasticsearch_data:
  prometheus_data:
  grafana_data:
```

**Dockerfile for Development**:

```dockerfile
# Dockerfile.dev
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline

COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-jammy

# Java 21 Virtual Threads optimization
ENV JAVA_OPTS="-XX:+UseZGC \
               -XX:+UnlockExperimentalVMOptions \
               --enable-preview \
               -XX:+UseStringDeduplication \
               -Xms512m -Xmx1g"

WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Hot reload support for development
COPY --from=build /app/target/classes ./classes

EXPOSE 8080

# Start Jakarta EE application
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Production Environment (Azure Container Apps)

**Azure Container Apps Environment Configuration**:

```yaml
# azure-container-apps.bicep
param environmentName string = 'ski-shop-env'
param location string = resourceGroup().location

// Container Apps Environment
resource containerAppsEnvironment 'Microsoft.App/managedEnvironments@2023-05-01' = {
  name: environmentName
  location: location
  properties: {
    vnetConfiguration: {
      infrastructureSubnetId: subnet.id
    }
    workloadProfiles: [
      {
        name: 'Consumption'
        workloadProfileType: 'Consumption'
      }
      {
        name: 'D4'
        workloadProfileType: 'D4'
        minimumCount: 1
        maximumCount: 10
      }
    ]
  }
}

// User Management Service
resource userManagementApp 'Microsoft.App/containerApps@2023-05-01' = {
  name: 'user-management-service'
  location: location
  properties: {
    managedEnvironmentId: containerAppsEnvironment.id
    workloadProfileName: 'D4'
    configuration: {
      activeRevisionsMode: 'Multiple'
      ingress: {
        external: false
        targetPort: 8080
        traffic: [
          {
            revisionName: 'user-management-service--latest'
            weight: 100
          }
        ]
      }
      secrets: [
        {
          name: 'database-connection-string'
          value: 'postgresql://...'
        }
      ]
    }
    template: {
      containers: [
        {
          name: 'user-management'
          image: 'skiShopRegistry.azurecr.io/user-management:latest'
          env: [
            {
              name: 'DATASOURCE_URL'
              secretRef: 'database-connection-string'
            }
            {
              name: 'JAVA_OPTS'
              value: '-XX:+UseZGC -XX:+UnlockExperimentalVMOptions --enable-preview'
            }
          ]
          resources: {
            cpu: '1.0'
            memory: '2Gi'
          }
          probes: [
            {
              type: 'Liveness'
              httpGet: {
                path: '/health/live'
                port: 8080
              }
              initialDelaySeconds: 30
              periodSeconds: 10
            }
            {
              type: 'Readiness'
              httpGet: {
                path: '/health/ready'
                port: 8080
              }
              initialDelaySeconds: 5
              periodSeconds: 5
            }
          ]
        }
      ]
      scale: {
        minReplicas: 2
        maxReplicas: 20
        rules: [
          {
            name: 'http-scaling'
            http: {
              metadata: {
                concurrentRequests: '100'
              }
            }
          }
          {
            name: 'cpu-scaling'
            custom: {
              type: 'cpu'
              metadata: {
                type: 'Utilization'
                value: '70'
              }
            }
          }
        ]
      }
    }
  }
}
```

**Infrastructure as Code (Bicep)**:

```bicep
// main.bicep
targetScope = 'resourceGroup'

param location string = resourceGroup().location
param environment string = 'production'

// Azure Database for PostgreSQL
module database 'modules/database.bicep' = {
  name: 'database'
  params: {
    location: location
    environment: environment
    serverName: 'ski-shop-postgres-${environment}'
    administratorLogin: 'skiShopadmin'
    databases: [
      'ski_shop_users'
      'ski_shop_products'
      'ski_shop_orders'
      'ski_shop_inventory'
    ]
  }
}

// Azure Cache for Redis
module redis 'modules/redis.bicep' = {
  name: 'redis'
  params: {
    location: location
    environment: environment
    redisCacheName: 'ski-shop-redis-${environment}'
    sku: {
      name: 'Standard'
      family: 'C'
      capacity: 2
    }
  }
}

// Azure Service Bus
module serviceBus 'modules/servicebus.bicep' = {
  name: 'serviceBus'
  params: {
    location: location
    environment: environment
    namespaceName: 'ski-shop-servicebus-${environment}'
    topics: [
      'order-events'
      'inventory-events'
      'user-events'
    ]
  }
}

// Container Registry
module containerRegistry 'modules/acr.bicep' = {
  name: 'containerRegistry'
  params: {
    location: location
    registryName: 'skiShopRegistry${environment}'
  }
}
```

## Handling Non-Functional Requirements

### Performance Requirements

**Response Time Goals**:

| Feature | Target Response Time | Measurement Method |
|------|------------------|---------|
| Product Search | < 500ms (95th percentile) | APM Monitoring |
| Product Detail View | < 300ms (95th percentile) | APM Monitoring |
| Cart Operations | < 200ms (95th percentile) | APM Monitoring |
| Order Processing | < 2s (average) | APM Monitoring |
| User Authentication | < 1s (95th percentile) | APM Monitoring |

**Throughput Requirements**:

```java
// Example of performance test implementation
@ApplicationScoped
public class PerformanceTestService {
    
    // High throughput realization with Virtual Threads
    private final Executor virtualThreadExecutor = 
        Executors.newVirtualThreadPerTaskExecutor();
    
    @Asynchronous
    public CompletableFuture<List<ProductSearchResult>> 
            performBulkSearch(List<SearchRequest> requests) {
        
        var futures = requests.stream()
            .map(request -> CompletableFuture.supplyAsync(
                () -> productSearchService.search(request),
                virtualThreadExecutor
            ))
            .toList();
            
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList()));
    }
}
```

### Scalability Support

**Horizontal Scaling Strategy**:

```yaml
# Azure Container Apps Auto Scaling
resources:
  user-management-service:
    scaling:
      minReplicas: 2
      maxReplicas: 50
      rules:
        - name: http-requests
          type: http
          metadata:
            concurrentRequests: "100"
        - name: cpu-utilization
          type: cpu
          metadata:
            type: "Utilization"
            value: "70"
        - name: memory-utilization
          type: memory
          metadata:
            type: "Utilization"
            value: "80"

  product-catalog-service:
    scaling:
      minReplicas: 3
      maxReplicas: 100
      rules:
        - name: search-requests
          type: http
          metadata:
            concurrentRequests: "200"
```

### Availability and Reliability

**High Availability Architecture**:

```java
// Circuit Breaker pattern implementation
@ApplicationScoped
public class ProductService {
    
    @Inject
    @CircuitBreaker(
        requestVolumeThreshold = 20,
        failureRatio = 0.5,
        delay = 5000,
        successThreshold = 3
    )
    @Retry(maxRetries = 3, delay = 1000)
    @Timeout(value = 3000)
    public Product getProduct(UUID productId) {
        return productRepository.findById(productId)
            .orElseThrow(() -> new ProductNotFoundException(productId));
    }
    
    // Fallback implementation
    @Fallback
    public Product getProductFallback(UUID productId) {
        // Get from cache or return default value
        return productCacheService.getFromCache(productId)
            .orElse(Product.createUnavailable(productId));
    }
}
```

### Security Requirement Handling

**Multi-layered Defense Strategy**:

```java
// Jakarta Security implementation
@ApplicationScoped
@DeclareRoles({"user", "admin", "premium"})
public class SecurityConfiguration {
    
    @Produces
    @ApplicationScoped
    public DatabaseIdentityStore createIdentityStore() {
        return DatabaseIdentityStore.builder()
            .dataSource(dataSource)
            .callerQuery("SELECT password FROM users WHERE email = ?")
            .groupsQuery("SELECT role FROM user_roles WHERE email = ?")
            .hashAlgorithm(Pbkdf2PasswordHash.class)
            .priority(10)
            .build();
    }
    
    @Produces
    @ApplicationScoped
    public RememberMeIdentityStore createRememberMeStore() {
        return new JWTRememberMeIdentityStore();
    }
}

// MicroProfile JWT authorization control
@Path("/admin")
@RolesAllowed("admin")
public class AdminResource {
    
    @Inject
    private JsonWebToken jwt;  // MicroProfile JWT token
    
    @GET
    @Path("/users")
    @RolesAllowed({"admin", "support"})
    public Response getAllUsers() {
        // Get user information from MicroProfile JWT
        String userId = jwt.getSubject();
        Set<String> groups = jwt.getGroups();
        
        // Accessible only by administrators
        return Response.ok().build();
    }
    
    @GET
    @Path("/profile")
    public Response getUserProfile() {
        // Directly inject claim value with @Claim annotation
        String username = jwt.claim("preferred_username").orElse("unknown");
        
        return Response.ok()
            .entity(Map.of("user", username, "roles", jwt.getGroups()))
            .build();
    }
}
```

## Infrastructure

### Azure Cloud Architecture

```yaml
# Kubernetes cluster configuration
apiVersion: v1
kind: Namespace
metadata:
  name: ski-shop

---
# User Management Service Deployment
apiVersion: apps/v1
kind: Deployment
metadata:
  name: user-management-service
  namespace: ski-shop
spec:
  replicas: 3
  selector:
    matchLabels:
      app: user-management-service
  template:
    metadata:
      labels:
        app: user-management-service
    spec:
      containers:
      - name: user-management
        image: skiShop/user-management:latest
        ports:
        - containerPort: 8080
        env:
        - name: DATASOURCE_URL
          valueFrom:
            secretKeyRef:
              name: database-secret
              key: url
        - name: JAVA_OPTS
          value: "-XX:+UseZGC -XX:+UnlockExperimentalVMOptions --enable-preview"
        resources:
          requests:
            memory: "512Mi"
            cpu: "500m"
          limits:
            memory: "1Gi"
            cpu: "1000m"
        livenessProbe:
          httpGet:
            path: /health/live
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /health/ready
            port: 8080
          initialDelaySeconds: 5
          periodSeconds: 5
```

### Monitoring and Observability

**Metrics Collection**:

- MicroProfile Metrics 5.1
- Prometheus + Grafana
- Custom Business Metrics

**Distributed Tracing**:

- MicroProfile OpenTelemetry 2.0
- Jaeger
- Azure Application Insights

**Log Management**:

- Structured Logging (JSON)
- ELK Stack (Elasticsearch, Logstash, Kibana)
- Azure Log Analytics

## Operation and Monitoring

### SRE (Site Reliability Engineering)

**SLI/SLO Settings**:

```yaml
# Service Level Indicators/Objectives
services:
  api-gateway:
    availability:
      sli: "success_rate"
      slo: "99.9%"
    latency:
      sli: "p95_response_time"
      slo: "< 500ms"
  
  user-management:
    availability:
      sli: "success_rate"  
      slo: "99.5%"
    latency:
      sli: "p99_response_time"
      slo: "< 1000ms"
```

**Alert Settings**:

```yaml
# Prometheus Alert Rules
groups:
- name: ski-shop-alerts
  rules:
  - alert: HighErrorRate
    expr: rate(http_requests_total{status=~"5.."}[5m]) > 0.1
    for: 2m
    labels:
      severity: critical
    annotations:
      summary: "High error rate detected"
      
  - alert: HighLatency
    expr: histogram_quantile(0.95, rate(http_request_duration_seconds_bucket[5m])) > 0.5
    for: 5m
    labels:
      severity: warning
    annotations:
      summary: "High latency detected"
```

### Disaster Recovery

**Backup Strategy**:

- **PostgreSQL**: Point-in-time Recovery (PITR)
- **Redis**: AOF + RDB
- **MongoDB**: Replica Set + Sharding

**RTO/RPO Goals**:

- **RTO**: Within 4 hours
- **RPO**: Within 15 minutes

## Development and Operation Process

### Agile Development Process

**Scrum Adoption**:

```yaml
# Sprint configuration
sprint_duration: 2 weeks
team_structure:
  - product_owner: 1
  - scrum_master: 1
  - developers: 6-8
  - qa_engineers: 2
  - devops_engineers: 2

ceremonies:
  daily_standup:
    duration: 15 minutes
    time: "09:00"
    
  sprint_planning:
    duration: 4 hours
    participants: "entire team"
    
  sprint_review:
    duration: 2 hours
    stakeholders: "PO + stakeholders"
    
  retrospective:
    duration: 1.5 hours
    participants: "development team"
```

**Definition of Done (DoD)**:

```markdown
## Definition of Done Checklist

### Code Quality
- [ ] Code review completed (approved by 2 or more people)
- [ ] Unit test coverage 80% or more
- [ ] Integration tests run and passed
- [ ] SonarQube quality gate passed
- [ ] Security scan run and no issues

### Documentation
- [ ] API specification updated
- [ ] README updated
- [ ] CHANGELOG updated
- [ ] Operation manual updated (if necessary)

### Testing and Quality Assurance
- [ ] Functional testing completed
- [ ] Performance testing run (if necessary)
- [ ] Accessibility testing run
- [ ] Browser compatibility confirmed

### Deployment
- [ ] Deployed to staging environment and confirmed
- [ ] Production release plan approved
- [ ] Rollback procedure confirmed
```
