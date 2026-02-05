#!/bin/bash
# scripts/check-diff-coverage.sh - Check incremental code coverage using diff-cover

set -e

echo "📊 Checking coverage for changed code..."

# Check if we're in the right directory
if [ ! -d "backend" ]; then
    echo "❌ Please run this script from the project root directory"
    exit 1
fi

cd backend

# Check if diff-cover is installed
if ! command -v diff-cover &> /dev/null; then
    echo "❌ diff-cover is not installed!"
    echo "Install it with: pip install diff-cover"
    exit 1
fi

# Generate JaCoCo report
echo "📈 Running tests and generating coverage report..."
./mvnw test jacoco:report -q

# Go back to project root
cd ..

# Determine what to compare against
JAVA_CHANGES=$( (git diff --name-only HEAD && git diff --cached --name-only) | grep 'src/main/java/.*\.java$' || true )

if [ -z "$JAVA_CHANGES" ]; then
    # No uncommitted changes - check last commit
    echo ""
    echo "🔍 Checking coverage for last commit (HEAD~1)..."
    echo "📝 Changed source files:"
    git diff --name-only HEAD~1 HEAD | grep 'src/main/java/.*\.java$' || echo "  (no source Java files changed)"
    echo ""
    BASE="HEAD~1"
else
    # Has uncommitted changes - check those
    echo ""
    echo "🔍 Checking coverage for uncommitted changes..."
    echo "📝 Changed source files:"
    echo "$JAVA_CHANGES" | sort -u
    echo ""
    BASE="HEAD"
fi

# Run diff-cover with the correct parameters
echo "📋 Coverage Report:"
echo "-------------"
diff-cover --compare-branch="$BASE" --src-roots=backend/src/main/java --fail-under=70 backend/target/site/jacoco/jacoco.xml
EXIT_CODE=$?

echo "-------------"

if [ $EXIT_CODE -eq 0 ]; then
    echo ""
    echo "✅ Coverage check PASSED!"
    echo "   Changed code meets the 70% coverage threshold."
else
    echo ""
    echo "❌ Coverage check FAILED!"
    echo "   Changed code does not meet the 70% coverage threshold."
    echo ""
    echo "View detailed report:"
    echo "  open backend/target/site/jacoco/index.html"
fi

exit $EXIT_CODE
