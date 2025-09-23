#!/bin/bash

# Customer Tracker Backend Startup Script
set -e

# Get script directory and navigate to project root
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR/.."

BACKEND_DIR="backend"
LOG_FILE="backend_run.log"

echo "☕ Starting Customer Tracker Backend (Spring Boot)..."

# Navigate to backend directory
cd "$BACKEND_DIR"

# Initialize SDKMAN and set Java 17
if [ -f "$HOME/.sdkman/bin/sdkman-init.sh" ]; then
    source "$HOME/.sdkman/bin/sdkman-init.sh"
    echo "☕ Using Java 17 via SDKMAN"
fi

# Check if Maven is available, try local installation first
if ! command -v mvn &> /dev/null; then
    # Try to use local Maven installation
    if [ -f "../apache-maven-3.9.6/bin/mvn" ]; then
        echo "🔧 Using local Maven installation"
        export PATH="../apache-maven-3.9.6/bin:$PATH"
    else
        echo "❌ Maven is not installed or not in PATH"
        echo "Please install Maven or run the start-all script from the project root"
        exit 1
    fi
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