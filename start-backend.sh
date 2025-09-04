#!/bin/bash

# Customer Tracker Backend Startup Script
set -e

BACKEND_DIR="backend"
LOG_FILE="backend_run.log"

echo "☕ Starting Customer Tracker Backend (Spring Boot)..."

# Navigate to backend directory
cd "$BACKEND_DIR"

# Check if Maven is available
if ! command -v mvn &> /dev/null; then
    echo "❌ Maven is not installed or not in PATH"
    exit 1
fi

# Clean and compile the project
echo "🔨 Building backend..."
mvn clean compile -q

# Start the Spring Boot application
echo "🚀 Starting Spring Boot application on port 8080..."
echo "📋 Logs will be written to ../$LOG_FILE"

# Run the application and redirect output to log file
mvn spring-boot:run > "../$LOG_FILE" 2>&1 &
BACKEND_PID=$!

# Save PID for later cleanup
echo $BACKEND_PID > ../backend.pid

echo "✅ Backend started with PID: $BACKEND_PID"
echo "🌐 Backend API will be available at: http://localhost:8080"
echo "📋 View logs with: tail -f $LOG_FILE"