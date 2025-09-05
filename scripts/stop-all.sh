#!/bin/bash

# Customer Tracker - Stop All Services
set -e

# Get script directory and navigate to project root
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR/.."

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}🛑 Customer Tracker - Stopping All Services${NC}"
echo "============================================="

# Stop backend
if [ -f backend.pid ]; then
    BACKEND_PID=$(cat backend.pid)
    if ps -p $BACKEND_PID > /dev/null 2>&1; then
        echo -e "${YELLOW}Stopping backend (PID: $BACKEND_PID)...${NC}"
        kill $BACKEND_PID 2>/dev/null || true
        sleep 2
        if ps -p $BACKEND_PID > /dev/null 2>&1; then
            echo -e "${RED}Force killing backend...${NC}"
            kill -9 $BACKEND_PID 2>/dev/null || true
        fi
        echo -e "${GREEN}✅ Backend stopped${NC}"
    else
        echo -e "${YELLOW}Backend was not running${NC}"
    fi
    rm -f backend.pid
else
    echo -e "${YELLOW}No backend PID file found${NC}"
fi

# Stop frontend
if [ -f frontend.pid ]; then
    FRONTEND_PID=$(cat frontend.pid)
    if ps -p $FRONTEND_PID > /dev/null 2>&1; then
        echo -e "${YELLOW}Stopping frontend (PID: $FRONTEND_PID)...${NC}"
        kill $FRONTEND_PID 2>/dev/null || true
        sleep 2
        if ps -p $FRONTEND_PID > /dev/null 2>&1; then
            echo -e "${RED}Force killing frontend...${NC}"
            kill -9 $FRONTEND_PID 2>/dev/null || true
        fi
        echo -e "${GREEN}✅ Frontend stopped${NC}"
    else
        echo -e "${YELLOW}Frontend was not running${NC}"
    fi
    rm -f frontend.pid
else
    echo -e "${YELLOW}No frontend PID file found${NC}"
fi

# Optionally stop database container
read -p "Do you want to stop the database container? (y/N): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo -e "${YELLOW}Stopping database container...${NC}"
    podman stop customer-tracker-db 2>/dev/null || true
    echo -e "${GREEN}✅ Database container stopped${NC}"
else
    echo -e "${BLUE}Database container left running${NC}"
fi

echo -e "${GREEN}🎉 All services stopped successfully!${NC}"