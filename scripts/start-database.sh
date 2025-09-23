#!/bin/bash

# Customer Tracker Database Startup Script  
set -e

# Get script directory and navigate to project root
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR/.."

CONTAINER_NAME="customer-tracker-db"
DB_NAME="customers"
DB_USER="postgres"
DB_PASSWORD="postgres"
DB_PORT="5432"

# Detect container runtime (Docker or Podman)
CONTAINER_RUNTIME=""
if command -v docker >/dev/null 2>&1 && docker info >/dev/null 2>&1; then
    CONTAINER_RUNTIME="docker"
    echo "🐳 Using Docker as container runtime"
elif command -v podman >/dev/null 2>&1; then
    CONTAINER_RUNTIME="podman"
    echo "🦭 Using Podman as container runtime"
else
    echo "❌ Neither Docker nor Podman is available or running"
    echo "Please install Docker Desktop or Podman to continue"
    exit 1
fi

echo "🗄️  Starting PostgreSQL database for Customer Tracker..."

# Check if container already exists and is running
if $CONTAINER_RUNTIME ps -q -f name="$CONTAINER_NAME" | grep -q .; then
    echo "✅ Database container is already running"
    exit 0
fi

# Check if container exists but is stopped
if $CONTAINER_RUNTIME ps -aq -f name="$CONTAINER_NAME" | grep -q .; then
    echo "🔄 Starting existing database container..."
    $CONTAINER_RUNTIME start "$CONTAINER_NAME"
else
    echo "🆕 Creating new PostgreSQL database container..."
    $CONTAINER_RUNTIME run -d \
        --name "$CONTAINER_NAME" \
        -e POSTGRES_DB="$DB_NAME" \
        -e POSTGRES_USER="$DB_USER" \
        -e POSTGRES_PASSWORD="$DB_PASSWORD" \
        -p "$DB_PORT:5432" \
        --restart=unless-stopped \
        postgres:15-alpine
fi

# Wait for database to be ready
echo "⏳ Waiting for database to be ready..."
for i in {1..30}; do
    if $CONTAINER_RUNTIME exec "$CONTAINER_NAME" pg_isready -U "$DB_USER" -d "$DB_NAME" >/dev/null 2>&1; then
        echo "✅ Database is ready!"
        break
    fi
    if [ $i -eq 30 ]; then
        echo "❌ Database failed to start within 30 seconds"
        exit 1
    fi
    sleep 1
done

echo "🗄️  PostgreSQL database is running on localhost:$DB_PORT"
echo "   Database: $DB_NAME"
echo "   Username: $DB_USER"
echo "   Password: $DB_PASSWORD"