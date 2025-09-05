#!/bin/bash
# scripts/pre-push.sh - Automated pre-push verification

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Header
echo -e "${BLUE}🚀 Customer Tracker - Pre-Push Verification${NC}"
echo "=============================================="
echo ""

# Check if we're in git repository
if [ ! -d ".git" ]; then
    echo -e "${RED}❌ Not in a git repository${NC}"
    exit 1
fi

# Check for uncommitted changes
if [ -n "$(git status --porcelain)" ]; then
    echo -e "${YELLOW}⚠️  You have uncommitted changes${NC}"
    echo "Please commit your changes before running pre-push checks"
    git status --short
    exit 1
fi

# Frontend verification
echo -e "${BLUE}📱 Frontend Quality Verification${NC}"
echo "--------------------------------"

cd frontend

echo -n "  🔍 Linting... "
if npm run lint --silent >/dev/null 2>&1; then
    echo -e "${GREEN}✅${NC}"
else
    echo -e "${RED}❌${NC}"
    echo -e "${RED}Linting failed. Run 'npm run lint' for details.${NC}"
    exit 1
fi

echo -n "  📝 Type checking... "
if npm run type-check --silent >/dev/null 2>&1; then
    echo -e "${GREEN}✅${NC}"
else
    echo -e "${RED}❌${NC}"
    echo -e "${RED}Type checking failed. Run 'npm run type-check' for details.${NC}"
    exit 1
fi

echo -n "  🏗️ Build testing... "
if npm run build --silent >/dev/null 2>&1; then
    echo -e "${GREEN}✅${NC}"
else
    echo -e "${RED}❌${NC}"
    echo -e "${RED}Build failed. Run 'npm run build' for details.${NC}"
    exit 1
fi

cd ..

# Backend verification
echo ""
echo -e "${BLUE}🔧 Backend Quality Verification${NC}"
echo "-------------------------------"

cd backend

echo -n "  🎨 Code formatting... "
if mvn spotless:apply -q >/dev/null 2>&1; then
    echo -e "${GREEN}✅${NC}"
else
    echo -e "${RED}❌${NC}"
    echo -e "${RED}Formatting failed. Check Java syntax.${NC}"
    exit 1
fi

echo -n "  📏 Format verification... "
if mvn spotless:check -q >/dev/null 2>&1; then
    echo -e "${GREEN}✅${NC}"
else
    echo -e "${RED}❌${NC}"
    echo -e "${RED}Format verification failed.${NC}"
    exit 1
fi

echo -n "  🏗️ Compilation... "
if mvn clean compile -q >/dev/null 2>&1; then
    echo -e "${GREEN}✅${NC}"
else
    echo -e "${RED}❌${NC}"
    echo -e "${RED}Compilation failed. Check Java syntax.${NC}"
    exit 1
fi

echo -n "  🧪 Unit tests... "
if mvn test -q >/dev/null 2>&1; then
    echo -e "${GREEN}✅${NC}"
else
    echo -e "${RED}❌${NC}"
    echo -e "${RED}Tests failed. Run 'mvn test' for details.${NC}"
    exit 1
fi

cd ..

# Success message
echo ""
echo -e "${GREEN}🎉 All quality checks passed!${NC}"
echo -e "${GREEN}✅ Code is ready for push to GitHub${NC}"
echo ""
echo -e "${BLUE}📤 Ready to push:${NC}"
echo "git push origin $(git branch --show-current)"
echo ""