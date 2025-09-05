#!/bin/bash

# Customer Tracker - Full Stack Startup Script
set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}🚀 Customer Tracker - Full Stack Startup${NC}"
echo "=========================================="

# Get script directory and navigate to project root
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR/.."

# Function to cleanup on exit
cleanup() {
    echo -e "\n${YELLOW}🧹 Cleaning up processes...${NC}"
    if [ -f backend.pid ]; then
        BACKEND_PID=$(cat backend.pid)
        if ps -p $BACKEND_PID > /dev/null 2>&1; then
            echo "Stopping backend (PID: $BACKEND_PID)..."
            kill $BACKEND_PID 2>/dev/null || true
        fi
        rm -f backend.pid
    fi
    
    if [ -f frontend.pid ]; then
        FRONTEND_PID=$(cat frontend.pid)
        if ps -p $FRONTEND_PID > /dev/null 2>&1; then
            echo "Stopping frontend (PID: $FRONTEND_PID)..."
            kill $FRONTEND_PID 2>/dev/null || true
        fi
        rm -f frontend.pid
    fi
    
    echo -e "${GREEN}✅ Cleanup completed${NC}"
}

# Set trap to cleanup on script exit
trap cleanup EXIT

# Function to wait for service to be ready
wait_for_service() {
    local url=$1
    local name=$2
    local max_attempts=$3
    
    echo -e "${YELLOW}⏳ Waiting for $name to be ready...${NC}"
    for i in $(seq 1 $max_attempts); do
        if curl -s "$url" > /dev/null 2>&1; then
            echo -e "${GREEN}✅ $name is ready!${NC}"
            return 0
        fi
        sleep 2
    done
    echo -e "${RED}❌ $name failed to start within expected time${NC}"
    return 1
}

# Step 1: Start Database
echo -e "${BLUE}Step 1: Starting PostgreSQL Database${NC}"
./start-database.sh
echo ""

# Step 2: Start Backend
echo -e "${BLUE}Step 2: Starting Spring Boot Backend${NC}"
./start-backend.sh
echo ""

# Step 3: Wait for backend to be ready
wait_for_service "http://localhost:8080/health" "Backend" 30

# Step 4: Start Frontend
echo -e "${BLUE}Step 3: Starting Next.js Frontend${NC}"
"$SCRIPT_DIR/start-frontend.sh"
echo ""

# Step 5: Wait for frontend to be ready
wait_for_service "http://localhost:3000" "Frontend" 30

echo -e "${GREEN}🎉 Customer Tracker is now running!${NC}"
echo "=========================================="
echo -e "🌐 Frontend: ${BLUE}http://localhost:3000${NC}"
echo -e "🔧 Backend API: ${BLUE}http://localhost:8080${NC}"
echo -e "🗄️  Database: ${BLUE}localhost:5432 (customers)${NC}"
echo ""
echo -e "${YELLOW}📋 Log files:${NC}"
echo "   Frontend: frontend_run.log"
echo "   Backend:  backend_run.log"
echo ""
echo -e "${YELLOW}💡 To stop all services, press Ctrl+C${NC}"
echo ""

# Keep the script running to monitor services
while true; do
    sleep 5
    
    # Check if backend is still running
    if [ -f backend.pid ]; then
        BACKEND_PID=$(cat backend.pid)
        if ! ps -p $BACKEND_PID > /dev/null 2>&1; then
            echo -e "${RED}❌ Backend process died unexpectedly${NC}"
            break
        fi
    fi
    
    # Check if frontend is still running
    if [ -f frontend.pid ]; then
        FRONTEND_PID=$(cat frontend.pid)
        if ! ps -p $FRONTEND_PID > /dev/null 2>&1; then
            echo -e "${RED}❌ Frontend process died unexpectedly${NC}"
            break
        fi
    fi
done