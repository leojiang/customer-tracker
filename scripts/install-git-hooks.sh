#!/bin/bash
# scripts/install-git-hooks.sh - Install automatic quality checking git hooks

set -e

echo "🔧 Installing Git Hooks for Automatic Quality Checks"
echo "====================================================="

# Get script directory and project root
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$SCRIPT_DIR/.."

# Create hooks directory if it doesn't exist
mkdir -p "$PROJECT_ROOT/.git/hooks"

# Install pre-push hook
echo "📤 Installing pre-push hook..."
cat > "$PROJECT_ROOT/.git/hooks/pre-push" << 'EOF'
#!/bin/bash
# Automatic pre-push quality verification

echo "🔍 Running automatic pre-push quality checks..."

# Get script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)/scripts"

# Run quality checks
if ! "$SCRIPT_DIR/quality-check.sh"; then
    echo "❌ Quality checks failed! Push aborted."
    echo "🔧 Fix the issues above and try pushing again."
    echo ""
    echo "To skip this check (NOT RECOMMENDED):"
    echo "git push --no-verify origin <branch>"
    exit 1
fi

echo "✅ Quality checks passed! Proceeding with push..."
EOF

# Make hook executable
chmod +x "$PROJECT_ROOT/.git/hooks/pre-push"

# Install pre-commit hook (optional)
echo "📝 Installing pre-commit hook..."
cat > "$PROJECT_ROOT/.git/hooks/pre-commit" << 'EOF'
#!/bin/bash
# Automatic pre-commit formatting

# Get script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)/scripts"
PROJECT_ROOT="$SCRIPT_DIR/.."

echo "🎨 Running automatic code formatting..."

# Backend formatting
if [ -d "$PROJECT_ROOT/backend" ]; then
    echo "  🔧 Formatting Java code..."
    cd "$PROJECT_ROOT/backend"
    mvn spotless:apply -q >/dev/null 2>&1 || true
    cd "$PROJECT_ROOT"
fi

# Frontend formatting (if you have prettier)
if [ -d "$PROJECT_ROOT/frontend" ] && [ -f "$PROJECT_ROOT/frontend/package.json" ]; then
    echo "  📱 Checking frontend formatting..."
    cd "$PROJECT_ROOT/frontend"
    # Add any frontend formatting here if needed
    cd "$PROJECT_ROOT"
fi

echo "✅ Formatting completed!"
EOF

# Make hook executable
chmod +x "$PROJECT_ROOT/.git/hooks/pre-commit"

echo ""
echo "✅ Git hooks installed successfully!"
echo ""
echo "📋 What happens now:"
echo "  📝 pre-commit:  Automatic code formatting before each commit"
echo "  📤 pre-push:    Quality checks before each push (can be bypassed with --no-verify)"
echo ""
echo "🔧 To uninstall hooks:"
echo "  rm .git/hooks/pre-push"
echo "  rm .git/hooks/pre-commit"
echo ""
echo "🚀 Try it: Make a change and run 'git push' - quality checks will run automatically!"