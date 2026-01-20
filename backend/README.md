# Customer Tracker Backend

Spring Boot 3.3.2 backend application for customer tracking system.

## Prerequisites

- **Java 17** or later
- **PostgreSQL 14** or later
- **Maven 3.9+** (or use the included Maven wrapper)

## Database Setup

### Option 1: Using Docker (Recommended)

```bash
# Start PostgreSQL container
docker run --name customer-tracker-db \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=customers \
  -p 5432:5432 \
  -d postgres:14

# Check if database is running
docker ps | grep customer-tracker-db

# View logs
docker logs customer-tracker-db
```

### Option 2: Local PostgreSQL

Install PostgreSQL locally and create a database:

```sql
CREATE DATABASE customers;
```

Update the database connection in `src/main/resources/application.yml` if needed.

## Building and Running

### Using Maven Wrapper (Recommended)

The project includes Maven wrapper, so you don't need to install Maven.

```bash
# On Linux/macOS
./mvnw clean install

# On Windows
mvnw.cmd clean install

# Run the application
./mvnw spring-boot:run
```

### Using Installed Maven

If you have Maven installed:

```bash
# Clean and build
mvn clean install

# Run the application
mvn spring-boot:run

# Or run the JAR directly
java -jar target/customers-0.0.1-SNAPSHOT.jar
```

### Using IntelliJ IDEA

1. **Import the project:**
   - File → Open → Select the `backend` directory
   - IntelliJ will automatically detect the Maven project
   - Wait for dependencies to download

2. **Configure the database:**
   - Ensure PostgreSQL is running on port 5432
   - The application will auto-run Flyway migrations on startup

3. **Run the application:**
   - Right-click on `CustomersApplication.java`
   - Select 'Run CustomersApplication'
   - Or use the Run Configuration dropdown

4. **Debug mode:**
   - Right-click on `CustomersApplication.java`
   - Select 'Debug CustomersApplication'

## Available Maven Commands

```bash
# Clean build
./mvnw clean

# Compile
./mvnw compile

# Run tests
./mvnw test

# Package (create JAR)
./mvnw package

# Run Spring Boot application
./mvnw spring-boot:run

# Format code with Spotless
./mvnw spotless:apply

# Check code format
./mvnw spotless:check

# Run Checkstyle
./mvnw checkstyle:check
```

## Application Endpoints

Once the application is running, it will be available at:

- **API Base URL:** http://localhost:8080
- **Swagger UI:** http://localhost:8080/swagger-ui/index.html
- **API Docs:** http://localhost:8080/v3/api-docs

## Configuration

### Application Configuration

Main configuration is in `src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/customers
    username: postgres
    password: postgres
  jpa:
    hibernate:
      ddl-auto: validate  # Use Flyway for schema management
  flyway:
    enabled: true

server:
  port: 8080
```

### Profiles

You can create different profiles for different environments:

- `application-dev.yml` - Development environment
- `application-test.yml` - Test environment
- `application-prod.yml` - Production environment

Example:
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

## Database Migrations

The project uses **Flyway** for database migrations. Migration files are located in:

```
src/main/resources/db/migration/
```

Naming convention: `V{version}__{description}.sql`

On startup, Flyway will automatically:
1. Check the `flyway_schema_history` table
2. Run any new migrations
3. Validate the schema

### Adding a New Migration

```bash
# Create a new migration file
touch src/main/resources/db/migration/V18__your_description.sql
```

## Testing

```bash
# Run all tests
./mvnw test

# Run specific test class
./mvnw test -Dtest=AuthServiceTest

# Run specific test method
./mvnw test -Dtest=AuthServiceTest#shouldLoginSuccessfullyWithValidCredentials
```

## Code Quality

### Spotless (Code Formatting)

```bash
# Check format
./mvnw spotless:check

# Apply formatting
./mvnw spotless:apply
```

### Checkstyle

```bash
# Check code style
./mvnw checkstyle:check

# View violations
./mvnw checkstyle:checkstyle
```

## Troubleshooting

### Port 8080 Already in Use

```bash
# Find process using port 8080
lsof -i :8080

# Kill the process
kill -9 <PID>
```

### Database Connection Issues

1. Verify PostgreSQL is running:
```bash
# Using Docker
docker ps | grep customer-tracker-db

# Using local PostgreSQL
pg_isready
```

2. Check connection:
```bash
psql -h localhost -p 5432 -U postgres -d customers
```

3. View Flyway migration history:
```sql
SELECT * FROM flyway_schema_history ORDER BY installed_on DESC;
```

### Maven Wrapper Issues

If the Maven wrapper doesn't work:

```bash
# Make mvnw executable
chmod +x mvnw

# Re-download wrapper
rm -rf .mvn/wrapper
./mvnw wrapper:wrapper
```

## Project Structure

```
backend/
├── src/
│   ├── main/
│   │   ├── java/com/example/customers/
│   │   │   ├── controller/     # REST controllers
│   │   │   ├── service/        # Business logic
│   │   │   ├── model/          # JPA entities
│   │   │   ├── repository/     # JPA repositories
│   │   │   └── CustomersApplication.java
│   │   └── resources/
│   │       ├── application.yml
│   │       └── db/migration/   # Flyway migrations
│   └── test/                   # Unit tests
├── .mvn/                       # Maven wrapper
├── mvnw                        # Maven wrapper script
├── pom.xml                     # Maven configuration
└── google_checks.xml           # Checkstyle configuration
```

## Development Tips

### Hot Reload with Spring DevTools

Add to `pom.xml`:
```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-devtools</artifactId>
  <scope>runtime</scope>
  <optional>true</optional>
</dependency>
```

### View SQL Queries

Add to `application.yml`:
```yaml
logging:
  level:
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
```

### Actuator Endpoints

Add to `pom.xml` for Spring Boot Actuator:
```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

Access at: http://localhost:8080/actuator

## API Documentation

Once running, visit the Swagger UI at:
```
http://localhost:8080/swagger-ui/index.html
```

This provides interactive API documentation for all endpoints.

## License

Proprietary - All rights reserved
