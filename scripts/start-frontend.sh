#!/bin/bash

# Customer Tracker Frontend Startup Script
set -e

# Get script directory and navigate to project root
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR/.."

FRONTEND_DIR="frontend"
LOG_FILE="frontend_run.log"

echo "⚛️  Starting Customer Tracker Frontend (Next.js)..."

# Navigate to frontend directory
cd "$FRONTEND_DIR"

# Check if npm is available
if ! command -v npm &> /dev/null; then
    echo "❌ npm is not installed or not in PATH"
    exit 1
fi

# Install dependencies if node_modules doesn't exist or is outdated
if [ ! -d "node_modules" ] || [ "../package-lock.json" -nt "node_modules" ]; then
    echo "📦 Installing/updating dependencies..."
    npm install
fi

# Start the development server
echo "🚀 Starting Next.js development server on port 3000..."
echo "📋 Logs will be written to ../$LOG_FILE"

# Run the development server and redirect output to log file
npm run dev > "../$LOG_FILE" 2>&1 &
FRONTEND_PID=$!

# Save PID for later cleanup
echo $FRONTEND_PID > ../frontend.pid

echo "✅ Frontend started with PID: $FRONTEND_PID"
echo "🌐 Frontend will be available at: http://localhost:3000"
echo "📋 View logs with: tail -f $LOG_FILE"