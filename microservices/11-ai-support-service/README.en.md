# AI Support Service

An AI support service using LangChain4j 1.1.0 + Azure OpenAI.

## Overview

This service is an AI support microservice built using the Quarkus framework.
It integrates LangChain4j 1.1.0 and Azure OpenAI (GPT-4o) to provide the following features:

- **Chatbot Functionality**: Customer support, product recommendations, technical advice
- **Product Recommendation System**: Profile-based, behavior-based, collaborative filtering
- **Search Functionality Enhancement**: Query expansion, synonym expansion, result re-ranking

## Technology Stack

- **Framework**: Quarkus 3.15.1
- **Language**: Java 21 LTS (with preview features enabled)
- **AI Integration**: LangChain4j 1.1.0
- **AI Provider**: Azure OpenAI GPT-4o
- **Container**: Docker & Docker Compose

## Utilization of Java 21 Features

This project actively utilizes the latest Java 21 features:

### 1. Records (Data Transfer Objects)
```java
public record ChatMessageRequest(
    @NotBlank String userId,
    @NotBlank String content,
    String conversationId,
    String sessionId,
    Map<String, Object> context
) {
    // With factory methods for validation and conversion
}
```

### 2. Sealed Classes/Interfaces (Type-Safe Hierarchies)
```java
public sealed interface Intent 
    permits Intent.ProductRecommendation, Intent.TechnicalAdvice, 
            Intent.OrderSupport, Intent.GeneralInquiry {
    
    record ProductRecommendation(String category, String preferences) implements Intent {}
    record TechnicalAdvice(String topic, String skillLevel) implements Intent {}
    // ...
}
```

### 3. Switch Expressions (Pattern Matching)
```java
return switch (intent) {
    case Intent.ProductRecommendation(var category, var preferences) -> 
        productRecommendationAssistant.recommend(message, category, preferences);
    case Intent.TechnicalAdvice(var topic, var skillLevel) -> 
        customerSupportAssistant.provideTechnicalAdvice(message, topic, skillLevel);
    // ...
};
```

### 4. Text Blocks (Multi-line Strings)
```java
String systemMessage = """
    You are a friendly ski shop assistant.
    Please keep the following points in mind when responding:
    - Use a polite and friendly tone.
    - Provide specific and practical advice.
    - Prioritize safety above all else.
    """;
```

## API Endpoints

### Chat API
- `POST /api/v1/chat/message` - General chat message processing
- `POST /api/v1/chat/recommend` - Chat specialized for product recommendations
- `POST /api/v1/chat/advice` - Chat specialized for technical advice
- `GET /api/v1/chat/conversations/{userId}` - Get conversation history
- `DELETE /api/v1/chat/conversations/{conversationId}` - Delete conversation

### Recommendation API
- `POST /api/v1/recommendations/profile-based` - Profile-based recommendation
- `POST /api/v1/recommendations/behavior-based` - Behavior-based recommendation
- `POST /api/v1/recommendations/collaborative` - Collaborative filtering recommendation
- `POST /api/v1/recommendations/bundle` - Product combination recommendation

### Search Enhancement API
- `POST /api/v1/search/enhance-query` - Query expansion
- `POST /api/v1/search/expand-synonyms` - Synonym expansion
- `POST /api/v1/search/rerank` - Search result re-ranking
- `POST /api/v1/search/analyze-intent` - Search intent analysis
- `POST /api/v1/search/personalized` - Personalized search

## Configuration

### Azure OpenAI Configuration
```yaml
azure:
  openai:
    endpoint: ${AZURE_OPENAI_ENDPOINT:https://your-openai-resource.openai.azure.com/}
    api-key: ${AZURE_OPENAI_API_KEY:your-api-key}
    deployment:
      chat: ${AZURE_OPENAI_CHAT_DEPLOYMENT:gpt-4o}
      embedding: ${AZURE_OPENAI_EMBEDDING_DEPLOYMENT:text-embedding-3-small}
```

### Database Configuration
```yaml
quarkus:
  datasource:
    db-kind: postgresql
    username: ${DB_USERNAME:ai_support}
    password: ${DB_PASSWORD:password}
    jdbc:
      url: ${DB_URL:jdbc:postgresql://localhost:5432/ai_support_db}
```

## How to Run

### Starting in Docker Environment (Recommended)

#### 1. Set Environment Variables

```bash
# Copy the environment variable template
cp .env.template .env

# Edit the .env file and enter your Azure OpenAI settings
vim .env
```

Example `.env` file configuration:

```bash
AZURE_OPENAI_ENDPOINT=https://your-resource-name.openai.azure.com/
AZURE_OPENAI_API_KEY=your-api-key-here
AZURE_OPENAI_CHAT_DEPLOYMENT_NAME=gpt-4o
AZURE_OPENAI_EMBEDDING_DEPLOYMENT_NAME=text-embedding-3-large
```

#### 2. Start the Service

**Easy Start (Recommended)**:

```bash
# Use the startup script (automatically runs up to health check)
./run-docker.sh
```

**Manual Start**:

```bash
# Start in the background
docker-compose up -d

# Check the logs
docker-compose logs -f ai-support-service

# Health check
curl http://localhost:8091/q/health
```

#### 3. Verify Operation

```bash
# API Documentation (Swagger UI)
open http://localhost:8091/q/swagger-ui

# Test chat functionality
curl -X POST http://localhost:8091/api/v1/chat/message \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user123",
    "content": "Please recommend some skis for me.",
    "conversationId": "conv456"
  }'
```

#### 4. Stop the Service

```bash
# Stop the service
docker-compose down

# Also remove volumes (complete cleanup)
docker-compose down -v
```

### Starting in Local Development Environment

#### Prerequisites

- Java 21 LTS (with preview features enabled)
- Maven 3.9 or higher
- Azure OpenAI service (API key and endpoint)

## Utilization of Java 21 Features

This service is implemented utilizing the latest features of Java 21:

- **Record Patterns**: Immutable data structures and pattern matching
- **Sealed Classes**: Type-safe hierarchical structures
- **Switch Expressions**: Enhanced switch statements and pattern matching
- **Text Blocks**: Clean description of multi-line strings
- **Virtual Threads**: High-performance concurrent processing (enabled in Quarkus)

#### Setting Environment Variables

```bash
export AZURE_OPENAI_ENDPOINT="https://your-resource-name.openai.azure.com/"
export AZURE_OPENAI_API_KEY="your-api-key-here"
export AZURE_OPENAI_CHAT_DEPLOYMENT_NAME="gpt-4o"
export AZURE_OPENAI_EMBEDDING_DEPLOYMENT_NAME="text-embedding-3-large"
```

### Development Mode

```bash
./mvnw compile quarkus:dev
```

### Production Build

```bash
./mvnw package
java -jar target/quarkus-app/quarkus-run.jar
```

### Native Build

```bash
./mvnw package -Dnative
./target/ai-support-service-1.0.0-SNAPSHOT-runner
```

## Verification Method

The functionality of this project can be verified using the following steps.

### Prerequisites

Set the Azure OpenAI environment variables:

```bash
export AZURE_OPENAI_API_KEY="your-api-key"
export AZURE_OPENAI_ENDPOINT="https://your-openai-resource.cognitiveservices.azure.com/"
export AZURE_OPENAI_CHAT_DEPLOYMENT_NAME="gpt-4o"
export AZURE_OPENAI_EMBEDDING_DEPLOYMENT_NAME="text-embedding-3-small"
```

### 1. Start the Application

Start in simple mode (no database or external services required):

```bash
mvn quarkus:dev -DskipTests=true -Dquarkus.http.port=8091
```

### 2. Health Check

Confirm the application's health:

```bash
curl http://localhost:8091/q/health
```

Expected result: `"status": "UP"`

### 3. Test Chat Functionality

Test the general chat functionality:

```bash
curl -X POST http://localhost:8091/api/v1/chat/message \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "test-user",
    "content": "Hello! Please tell me about skiing.",
    "conversationId": "test-conversation"
  }'
```

Expected result: A Japanese response from Azure OpenAI.

### 4. Test Product Recommendation Functionality

Test the chat specialized for product recommendations:

```bash
curl -X POST http://localhost:8091/api/v1/chat/recommend \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "test-user",
    "content": "Please recommend skis for a beginner. I am 170cm tall and have almost no skiing experience.",
    "conversationId": "recommend-conversation"
  }'
```

Expected result: Detailed product recommendations and suggestions by price range.

### 5. Test Technical Advice Functionality

Test the technical advice functionality:

```bash
curl -X POST http://localhost:8085/api/v1/chat/advice \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "test-user",
    "content": "I can't do parallel turns well. How should I practice?",
    "conversationId": "advice-conversation"
  }'
```

Expected result: Step-by-step practice methods and specific advice.

### 6. Test Search Enhancement Functionality

Test the search query expansion functionality:

```bash
curl -X POST "http://localhost:8091/api/v1/search/enhance-query" \
  -G --data-urlencode "query=skis for beginners"
```

Expected result: Analysis of the search query and improvement suggestions.

### 7. Check API Specification

Check the OpenAPI specification:

```bash
curl http://localhost:8091/q/openapi
```

Check from the browser with Swagger UI:

```text
http://localhost:8085/q/swagger-ui
```

### Verification Checklist

- [ ] The application starts normally (approx. 5-10 seconds)
- [ ] The health check returns a `UP` status
- [ ] The chat function responds appropriately in Japanese
- [ ] Product recommendations provide detailed and practical content
- [ ] Technical advice is step-by-step and considers safety
- [ ] The search enhancement function properly analyzes and expands queries
- [ ] The OpenAPI specification displays 15 or more endpoints
- [ ] Swagger UI displays normally

### Troubleshooting

#### If a Docker error occurs

Dev Services are disabled, so Docker is not required. The error message can be ignored.

#### If a port conflict occurs

Start on a different port:

```bash
mvn quarkus:dev -Dquarkus.http.port=8086
```

#### If an Azure OpenAI connection error occurs

- Check if the environment variables are set correctly
- Check if the Azure OpenAI API Key, Endpoint, and deployment name are accurate
- Check if the Azure OpenAI quota has remaining capacity

## Docker Run

```bash
# Build the image
docker build -f src/main/docker/Dockerfile.jvm -t ai-support-service .

# Run the container
docker run -i --rm -p 8091:8091 ai-support-service
```

## Health Check

- **Liveness**: `GET /q/health/live`
- **Readiness**: `GET /q/health/ready`

## API Documentation

In development mode, you can access Swagger UI at the following URL:

- <http://localhost:8091/q/swagger-ui>

## Key Implementation Classes

### Entities

- `ConversationSession` - Conversation session management
- `ChatMessage` - Chat message
- `ConversationAnalysis` - Conversation analysis result
- `KnowledgeBaseEntry` - Knowledge base entry

### Services

- `ChatService` - Main chat service
- `CustomerSupportAssistant` - Customer support AI
- `ProductRecommendationAssistant` - Product recommendation AI
- `SearchEnhancementAssistant` - Search enhancement AI

### Controllers

- `ChatController` - Chat API
- `RecommendationController` - Recommendation API
- `SearchController` - Search enhancement API

### Configuration & Exception Handling

- `LangChain4jConfig` - LangChain4j configuration
- `AiServiceException` - AI-related exception handling
- `GlobalExceptionHandler` - Global exception handling

## Features

1.  **Utilization of Latest Technology**
    -   Latest features of LangChain4j 1.1.0
    -   New features of Java 21 (Records, Sealed Classes, Switch Expressions, Text Blocks)
    -   Cloud-native support with Quarkus 3.15.1

2.  **Robust Design**
    -   Comprehensive error handling
    -   Retry mechanism and fallbacks
    -   Type-safe API design

3.  **High Performance**
    -   Fast startup with Quarkus
    -   Redis caching
    -   Asynchronous processing support

4.  **Extensibility**
    -   Microservice architecture
    -   Pluggable AI services
    -   Flexible configuration system

## License

This project is licensed under the MIT License.

## TODO

Currently, persistence and caching features are disabled to facilitate verification in the development environment.
In practice, a more robust system for a production environment can be built by configuring PostgreSQL, Redis, etc.

## Troubleshooting in Docker Environment

### Common Issues

1.  **Azure OpenAI Connection Error**

    ```bash
    # Check configuration
    docker-compose exec ai-support-service env | grep AZURE
    
    # Check logs
    docker-compose logs ai-support-service
    ```

2.  **Out of Memory Error**

    ```bash
    # Check resource usage
    docker stats ai-support-service
    
    # Adjust memory limit in docker-compose.yml
    ```

3.  **Port Conflict**

    ```bash
    # Check port usage
    lsof -i :8091
    
    # Change the port number in docker-compose.yml
    ports:
      - "8092:8091"  # Expose on port 8092
    ```

### Log Confirmation

```bash
# Real-time logs
docker-compose logs -f ai-support-service

# Past logs
docker-compose logs --tail=100 ai-support-service

# Error logs only
docker-compose logs ai-support-service 2>&1 | grep ERROR
```

### Monitoring & Metrics

```bash
# Health check
curl http://localhost:8091/q/health

# Metrics (Prometheus format)
curl http://localhost:8091/q/metrics

# Application information
curl http://localhost:8091/q/info
```
