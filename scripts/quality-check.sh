#!/bin/bash
# scripts/quality-check.sh - Quick local quality verification

set -e

echo "🚀 Customer Tracker - Quality Check"
echo "=================================="

# Check if we're in the right directory
if [ ! -f "package.json" ] || [ ! -d "backend" ] || [ ! -d "frontend" ]; then
    echo "❌ Please run this script from the project root directory"
    exit 1
fi

# Frontend checks
echo ""
echo "📱 Frontend Quality Checks..."
cd frontend

echo "  🔍 Linting..."
npm run lint --silent

echo "  📝 Type checking..."
npm run type-check --silent

echo "  🏗️ Build test..."
npm run build --silent >/dev/null 2>&1

echo "  ✅ Frontend checks passed!"
cd ..

# Backend checks  
echo ""
echo "🔧 Backend Quality Checks..."
cd backend

echo "  🎨 Code formatting..."
../apache-maven-3.9.6/bin/mvn spotless:apply -q

echo "  📏 Format verification..."
../apache-maven-3.9.6/bin/mvn spotless:check -q

echo "  🏗️ Compilation..."
../apache-maven-3.9.6/bin/mvn clean compile -q

echo "  🧪 Unit tests..."
../apache-maven-3.9.6/bin/mvn test -q

echo "  ✅ Backend checks passed!"
cd ..

echo ""
echo "🎉 All quality checks passed! Ready to push."
echo "📤 Run: git push origin $(git branch --show-current)"
echo ""