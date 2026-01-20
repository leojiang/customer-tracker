#!/bin/bash

# Customer Tracker Backend - Run Script

echo "🚀 Starting Customer Tracker Backend..."

# Check if Java is installed
if ! command -v java &> /dev/null; then
    echo "❌ Error: Java is not installed. Please install Java 17 or later."
    exit 1
fi

# Check Java version
JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 17 ]; then
    echo "❌ Error: Java 17 or later is required. Current version: $JAVA_VERSION"
    exit 1
fi

# Check if PostgreSQL is running (assuming default port)
if ! nc -z localhost 5432 2>/dev/null; then
    echo "⚠️  Warning: PostgreSQL does not appear to be running on port 5432"
    echo "   Please start PostgreSQL before continuing"
    echo ""
    echo "   Docker: docker run --name customer-tracker-db -e POSTGRES_PASSWORD=postgres -p 5432:5432 -d postgres:14"
    echo ""
    read -p "Press Enter to continue anyway, or Ctrl+C to exit..."
fi

# Make Maven wrapper executable
chmod +x mvnw 2>/dev/null

# Build and run
echo "📦 Building project..."
./mvnw clean compile

if [ $? -eq 0 ]; then
    echo "✅ Build successful"
    echo "🎯 Starting application..."
    echo ""
    echo "API will be available at: http://localhost:8080"
    echo "Swagger UI: http://localhost:8080/swagger-ui/index.html"
    echo ""
    ./mvnw spring-boot:run
else
    echo "❌ Build failed"
    exit 1
fi
