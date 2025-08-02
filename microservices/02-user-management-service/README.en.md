# User Management Service

The User Management Service is a microservice responsible for managing user information in the ski resort management system.

## Overview

This service provides the following user management functions:

- **User Profile Management**: Manages personal information, settings, and skill levels.
- **Role and Permission Management**: Controls permissions for customers, staff, administrators, etc.
- **Preference Management**: Manages personal settings, language, and notification preferences.
- **Account Lifecycle**: Activation, deactivation, and deletion.
- **Profile Verification**: Identity verification and skill level certification.
- **Data Privacy**: GDPR compliance and data deletion requests.

## Technology Stack

- **Jakarta EE 11**: Enterprise Java framework
- **Java 21 LTS**: Programming language
- **WildFly 31.0.1**: Application server
- **PostgreSQL**: Main database
- **Redis**: Cache and session management
- **MicroProfile Config**: Configuration management
- **MicroProfile Health**: Health checks
- **Bean Validation**: Data validation

## Architecture

```text
┌─────────────────────────────────────────────────────────┐
│                User Management Service                   │
├─────────────────────────────────────────────────────────┤
│  REST Layer (JAX-RS)                                   │
│  ├─ UserResource                                        │
│  ├─ ProfileResource                                     │
│  └─ Exception Handlers                                  │
├─────────────────────────────────────────────────────────┤
│  Service Layer                                          │
│  ├─ UserService                                         │
│  ├─ ProfileService                                      │
│  └─ PreferenceService                                   │
├─────────────────────────────────────────────────────────┤
│  Repository Layer                                       │
│  ├─ UserRepository                                      │
│  ├─ UserProfileRepository                               │
│  └─ UserPreferenceRepository                            │
├─────────────────────────────────────────────────────────┤
│  Entity Layer (JPA)                                     │
│  ├─ User                                                │
│  ├─ UserProfile                                         │
│  └─ UserPreference                                      │
└─────────────────────────────────────────────────────────┘
```

## Entity Design

### User (Basic User Information)

- User ID, username, email address
- Account status (active, suspended, disabled)
- Creation date, update date, last login
- Role (CUSTOMER, STAFF, ADMIN)

### UserProfile (User Profile)

- Personal information (name, date of birth, gender, phone number)
- Address information (country, prefecture, city, postal code)
- Ski information (level, years of experience, preferred slopes)
- Emergency contact information

### UserPreference (User Settings)

- Language settings, time zone
- Notification settings (email, SMS, push)
- Privacy settings
- Marketing consent settings

## API Endpoints

### User Management API

| Method | Endpoint | Description |
|---------|---------------|------|
| GET | `/users` | Get user list (admins only) |
| GET | `/users/{userId}` | Get user details |
| PUT | `/users/{userId}` | Update user information |
| DELETE | `/users/{userId}` | Delete user (logical deletion) |
| PUT | `/users/{userId}/status` | Change account status |

### Profile Management API

| Method | Endpoint | Description |
|---------|---------------|------|
| GET | `/users/{userId}/profile` | Get profile |
| PUT | `/users/{userId}/profile` | Update profile |
| POST | `/users/{userId}/profile/verify` | Upload identity document |

### Settings Management API

| Method | Endpoint | Description |
|---------|---------------|------|
| GET | `/users/{userId}/preferences` | Get settings |
| PUT | `/users/{userId}/preferences` | Update settings |

## Security Features

### Access Control

- Role-Based Access Control (RBAC)
- Access only to one's own information (admins are an exception)
- API key-based authentication (for inter-service communication)

### Data Privacy

- GDPR-compliant data processing
- Handling of data deletion requests
- Encryption of personal information

### Input Validation

- Strict validation with Jakarta Validation
- Protection against XSS, SQL injection
- File upload restrictions

## Configuration

### Environment Variables

| Variable Name | Description | Default Value |
|--------|------|-------------|
| `DATABASE_URL` | Database connection URL | `jdbc:postgresql://localhost:5432/skiresortdb` |
| `DATABASE_USER` | Database user | `skiresort` |
| `DATABASE_PASSWORD` | Database password | `skiresort` |
| `REDIS_HOST` | Redis host | `localhost` |
| `REDIS_PORT` | Redis port | `6379` |
| `FILE_UPLOAD_MAX_SIZE` | File upload limit | `10MB` |
| `PROFILE_PHOTO_PATH` | Profile photo storage path | `/var/uploads/profiles` |

## Database Configuration

### PostgreSQL Configuration

```sql
-- User management tables
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    role VARCHAR(20) NOT NULL DEFAULT 'CUSTOMER',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP
);

-- Profile table
CREATE TABLE user_profiles (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    birth_date DATE,
    phone_number VARCHAR(20),
    ski_level VARCHAR(20),
    profile_photo_url TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

## Build and Run

### Prerequisites

- Java 21 LTS
- Maven 3.9+
- PostgreSQL 15+
- Redis 7+
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
cp target/user-management-service.war $WILDFLY_HOME/standalone/deployments/
```

### Docker Run

```bash
# Run with Docker Compose
docker-compose up user-management-service
```

## API Usage Examples

### Get User Details

```bash
curl -X GET http://localhost:8081/users/123 \
  -H "Authorization: Bearer your_jwt_token"
```

### Update Profile

```bash
curl -X PUT http://localhost:8081/users/123/profile \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer your_jwt_token" \
  -d '{
    "firstName": "Taro",
    "lastName": "Tanaka",
    "phoneNumber": "090-1234-5678",
    "skiLevel": "INTERMEDIATE"
  }'
```

### Update Settings

```bash
curl -X PUT http://localhost:8081/users/123/preferences \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer your_jwt_token" \
  -d '{
    "language": "en",
    "timezone": "Asia/Tokyo",
    "emailNotifications": true,
    "smsNotifications": false
  }'
```

## Monitoring and Logging

### Health Check

- Check service status at the `/health` endpoint
- Database connection status
- Redis connection status

### Metrics

- Number of user registrations
- Profile update rate
- API usage statistics
- Response time

### Logs

- User operation logs
- Profile change history
- Security events

## Security Considerations

### Recommended Settings for Production Environment

1. **Database Encryption**
   - Encryption at rest (PII)
   - Encryption in transit (TLS)

2. **Enhanced Access Control**
   - Principle of least privilege
   - Regular review of access rights

3. **Audit Logs**
   - Recording of all change operations
   - Detection of unauthorized access

## Troubleshooting

### Common Issues

1. **Database Connection Error**
   - Check the connection string
   - Check authentication credentials

2. **File Upload Failure**
   - Check file size limits
   - Check disk space

3. **Permission Error**
   - Check the JWT token
   - Check role settings

## Future Expansion Plans

- [ ] Social profile integration
- [ ] Automated profile verification
- [ ] AI-based detection of fraudulent profiles
- [ ] Mobile app support
- [ ] Multilingual profile support

## Information for Developers

### Code Structure

```text
src/main/java/
├── com/skiresort/user/
│   ├── entity/          # JPA entities
│   ├── service/         # Business logic
│   ├── repository/      # Data access
│   ├── resource/        # REST endpoints
│   └── exception/       # Exception classes
```

### Dependencies

- Jakarta EE 11 API
- MicroProfile Config
- MicroProfile Health
- PostgreSQL JDBC
- Redis Client
- Apache Commons IO

## License

This project is released under the MIT License.
