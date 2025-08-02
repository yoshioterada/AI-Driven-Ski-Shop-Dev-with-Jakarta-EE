````markdown
# Authentication Service

The Authentication Service is a microservice responsible for central authentication in the ski resort management system.

## Overview

This service provides the following authentication functions:

- **Local Authentication**: Authentication using username/password
- **OAuth2 Authentication**: Social authentication via Google, Facebook, Twitter
- **Multi-Factor Authentication (MFA)**: Additional authentication via SMS, Email, TOTP
- **JWT Token Management**: Issuing and validating access tokens and refresh tokens
- **Password Management**: Password reset, strength check
- **Account Management**: Account lockout, email confirmation

## Technology Stack

- **Jakarta EE 11**: Enterprise Java framework
- **Java 21 LTS**: Programming language
- **WildFly 31.0.1**: Application server
- **PostgreSQL**: Main database
- **Redis**: Token cache and session management
- **JWT**: JSON Web Token implementation (Nimbus JOSE + JWT)
- **BCrypt**: Password hashing
- **MicroProfile Config**: Configuration management
- **MicroProfile JWT**: JWT authentication

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                Authentication Service                    │
├─────────────────────────────────────────────────────────┤
│  REST Layer (JAX-RS)                                   │
│  ├─ AuthenticationResource                              │
│  └─ Exception Handlers                                  │
├─────────────────────────────────────────────────────────┤
│  Service Layer                                          │
│  ├─ AuthenticationService                               │
│  ├─ JwtService                                          │
│  └─ OAuth2Service                                       │
├─────────────────────────────────────────────────────────┤
│  Repository Layer                                       │
│  ├─ UserCredentialRepository                            │
│  └─ OAuth2CredentialRepository                          │
├─────────────────────────────────────────────────────────┤
│  Entity Layer (JPA)                                     │
│  ├─ UserCredential                                      │
│  └─ OAuth2Credential                                    │
└─────────────────────────────────────────────────────────┘
```

## Entity Design

### UserCredential
- User ID, username, email, password
- MFA settings (SMS, Email, TOTP)
- Account status (active, locked, disabled)
- Password reset, email confirmation tokens
- Login attempt count, last login time

### OAuth2Credential
- OAuth2 provider information (Google, Facebook, Twitter)
- Access token, refresh token
- Provider user ID, profile information
- Token expiration, last sync time

## API Endpoints

### Authentication API

| Method | Endpoint | Description |
|---------|---------------|------|
| POST | `/auth/register` | User registration |
| POST | `/auth/login` | Username/password authentication |
| POST | `/auth/mfa/verify` | MFA code verification |
| POST | `/auth/oauth2/authenticate` | OAuth2 authentication |
| POST | `/auth/refresh` | Token refresh |
| POST | `/auth/revoke` | Token revocation |

### Password Management API

| Method | Endpoint | Description |
|---------|---------------|------|
| POST | `/auth/password/reset-request` | Password reset request |
| POST | `/auth/password/reset` | Execute password reset |

### Email Confirmation API

| Method | Endpoint | Description |
|---------|---------------|------|
| GET | `/auth/email/verify` | Verify email address |

## Security Features

### Password Security
- Strong hashing with BCrypt
- Password strength check (uppercase, lowercase, numbers, special characters)
- Password history management (prevents reuse)

### Account Protection
- Login attempt limit (default 5 times)
- Temporary account lockout (default 30 minutes)
- IP address-based rate limiting

### Multi-Factor Authentication (MFA)
- **SMS Authentication**: Sends SMS code via AWS SNS
- **Email Authentication**: Sends email code via SMTP
- **TOTP Authentication**: Supports apps like Google Authenticator

### OAuth2 Security
- PKCE (Proof Key for Code Exchange) support
- CSRF attack prevention with State parameter
- Scope-based access control

## JWT Token Design

### Access Token
- **Expiration**: 15 minutes (configurable)
- **Claims**: User ID, roles, permissions
- **Usage**: API authentication

### Refresh Token
- **Expiration**: 7 days (configurable)
- **Claims**: User ID, roles
- **Usage**: Access token renewal

## Configuration

### Environment Variables

| Variable Name | Description | Default Value |
|--------|------|-------------|
| `JWT_SECRET` | JWT signing key | `your-256-bit-secret-key-here-change-in-production` |
| `JWT_ACCESS_TOKEN_EXPIRATION` | Access token expiration | `PT15M` |
| `JWT_REFRESH_TOKEN_EXPIRATION` | Refresh token expiration | `P7D` |
| `MAX_LOGIN_ATTEMPTS` | Maximum login attempts | `5` |
| `LOCKOUT_DURATION` | Account lockout duration | `PT30M` |
| `MFA_ENABLED` | Enable MFA | `true` |
| `REDIS_HOST` | Redis host | `localhost` |
| `REDIS_PORT` | Redis port | `6379` |

### OAuth2 Configuration

Configuration for each OAuth2 provider:

```properties
# Google OAuth2
oauth2.google.client-id=${OAUTH2_GOOGLE_CLIENT_ID}
oauth2.google.client-secret=${OAUTH2_GOOGLE_CLIENT_SECRET}

# Facebook OAuth2
oauth2.facebook.client-id=${OAUTH2_FACEBOOK_CLIENT_ID}
oauth2.facebook.client-secret=${OAUTH2_FACEBOOK_CLIENT_SECRET}

# Twitter OAuth2
oauth2.twitter.client-id=${OAUTH2_TWITTER_CLIENT_ID}
oauth2.twitter.client-secret=${OAUTH2_TWITTER_CLIENT_SECRET}
```

## Database Configuration

### PostgreSQL Configuration

```sql
-- Create database
CREATE DATABASE ski_resort_auth;

-- Create user
CREATE USER auth_user WITH PASSWORD 'your_password';
GRANT ALL PRIVILEGES ON DATABASE ski_resort_auth TO auth_user;
```

### Redis Configuration

```
# redis.conf
maxmemory 256mb
maxmemory-policy allkeys-lru
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
cp target/authentication-service.war $WILDFLY_HOME/standalone/deployments/
```

### Run with Docker

```bash
# Run with Docker Compose
docker-compose up authentication-service
```

## API Usage Examples

### User Registration

```bash
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john_doe",
    "email": "john@example.com",
    "password": "SecurePass123!"
  }'
```

### Login

```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john_doe",
    "password": "SecurePass123!"
  }'
```

### OAuth2 Authentication (Google)

```bash
curl -X POST http://localhost:8080/auth/oauth2/authenticate \
  -H "Content-Type: application/json" \
  -d '{
    "provider": "google",
    "authorizationCode": "authorization_code_from_google",
    "redirectUri": "http://localhost:3000/callback"
  }'
```

### Token Refresh

```bash
curl -X POST http://localhost:8080/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "your_refresh_token"
  }'
```

## Monitoring and Logging

### Health Check
- `/health` endpoint for service status check
- Database connection status
- Redis connection status

### Metrics
- Authentication success/failure rate
- MFA usage rate
- Statistics by OAuth2 provider
- Response time

### Logs
- Authentication events (success, failure, MFA)
- Security events (account lockout, unauthorized access)
- System errors

## Security Considerations

### Recommended Settings for Production Environment

1. **Change JWT Signing Key**
   ```
   JWT_SECRET=your-strong-256-bit-secret-key-here
   ```

2. **Require HTTPS**
   - Encrypt all communication with HTTPS
   - Use Secure Cookies

3. **Strengthen Rate Limiting**
   - IP address-based limiting
   - User-based limiting

4. **Enhance Monitoring**
   - Detect abnormal login patterns
   - Configure security alerts

## Troubleshooting

### Common Issues

1. **JWT Signature Error**
   - Check if `JWT_SECRET` is set correctly
   - Check time synchronization

2. **OAuth2 Authentication Failure**
   - Check client ID and secret
   - Check redirect URI

3. **MFA Failure**
   - Check time synchronization (TOTP)
   - Check SMS provider settings

## Future Expansion Plans

- [ ] WebAuthn/FIDO2 support
- [ ] Risk-based authentication
- [ ] Single Sign-On (SSO)
- [ ] Improved session management
- [ ] Fraud detection with AI/ML

## For Developers

### Code Structure
```
src/main/java/
├── com/skiresort/auth/
│   ├── entity/          # JPA Entities
│   ├── service/         # Business Logic
│   ├── repository/      # Data Access
│   ├── resource/        # REST Endpoints
│   └── exception/       # Exception Classes
```

### Dependencies
- Jakarta EE 11 API
- MicroProfile Config
- MicroProfile JWT
- Nimbus JOSE + JWT
- BCrypt
- PostgreSQL JDBC
- Redis Client

## License

This project is licensed under the MIT License.
````
