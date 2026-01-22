#!/bin/bash

# Customer Tracker - Stop Backend Service
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

echo -e "${BLUE}🛑 Customer Tracker - Stopping Backend${NC}"
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
    echo -e "${YELLOW}The backend may not have been started with the start script${NC}"
fi

echo -e "${GREEN}🎉 Backend service stopped successfully!${NC}"
