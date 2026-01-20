# Quick Start Guide - Customer Tracker Backend

## 1. Start the Database (First Time Only)

### Option A: Using Docker Compose (Easiest)
```bash
docker-compose up -d
```

### Option B: Using Docker Command
```bash
docker run --name customer-tracker-db \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=customers \
  -p 5432:5432 \
  -d postgres:14
```

### Option C: Local PostgreSQL
Make sure PostgreSQL is running locally and create the database:
```sql
CREATE DATABASE customers;
```

## 2. Run the Application

### Option A: Using Run Script (Linux/macOS)
```bash
./run.sh
```

### Option B: Using Maven Wrapper
```bash
./mvnw spring-boot:run
```

### Option C: Using IntelliJ IDEA
1. Open the project in IntelliJ
2. Wait for Maven to import dependencies
3. Right-click on `CustomersApplication.java` → Run

## 3. Verify It's Running

Open your browser and visit:
- **Swagger UI:** http://localhost:8080/swagger-ui/index.html
- **Health Check:** http://localhost:8080/actuator/health (if actuator is enabled)

## Common Commands

```bash
# Compile
./mvnw compile

# Run tests
./mvnw test

# Build JAR
./mvnw package

# Format code
./mvnw spotless:apply

# Clean build
./mvnw clean
```

## Troubleshooting

### Database connection failed
```bash
# Check if PostgreSQL is running
docker ps | grep customer-tracker-db

# Start it if not running
docker-compose up -d
```

### Port 8080 already in use
```bash
# Find and kill process
lsof -i :8080
kill -9 <PID>
```

### Maven wrapper permission denied
```bash
chmod +x mvnw
```

## Project URLs

- API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui/index.html
- API Docs: http://localhost:8080/v3/api-docs

## Next Steps

1. Check the full [README.md](README.md) for detailed documentation
2. Explore the API in Swagger UI
3. Read the code in `src/main/java/com/example/customers/`
