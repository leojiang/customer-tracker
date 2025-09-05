# Development Workflow

> **Complete workflow guide for maintaining code quality and preventing CI/CD failures**

## 🚀 **Pre-Push Quality Checklist**

**⚠️ CRITICAL**: Always run these checks locally before pushing code to prevent CI/CD failures and maintain enterprise-grade code quality.

### **📱 Frontend Pre-Push Checklist**

#### **🔧 Required Commands**
```bash
cd frontend

# 1. Install/update dependencies  
npm ci

# 2. Linting (0 warnings, 0 errors required)
npm run lint

# 3. TypeScript compilation check
npm run type-check

# 4. Production build test
npm run build

# 5. Optional: Run auto-fix for minor issues
npm run lint:fix
```

#### **✅ Success Criteria**
- **✅ Linting**: `0 errors, 0 warnings` - Zero tolerance policy
- **✅ TypeScript**: No compilation errors - Type safety enforced
- **✅ Build**: Production build completes successfully
- **✅ Bundle Size**: No significant bundle size increases without justification

#### **📋 Frontend Quality Standards**
```json
{
  "linting": "ESLint + TypeScript strict mode",
  "formatting": "2-space indentation, semicolons, single quotes",
  "imports": "No unused imports allowed",
  "console": "No console.log statements in production code",
  "dependencies": "All useEffect hooks must have proper dependency arrays",
  "types": "Full TypeScript coverage, no 'any' types"
}
```

### **🔧 Backend Pre-Push Checklist**

#### **🔧 Required Commands**
```bash
cd backend

# 1. Code formatting (Google Java Style)
mvn spotless:apply

# 2. Verify formatting compliance
mvn spotless:check

# 3. Compilation check
mvn clean compile

# 4. Run all unit tests
mvn test

# 5. Optional: Full verification including integration tests
mvn clean verify
```

#### **✅ Success Criteria**
- **✅ Spotless**: All Java files properly formatted with Google Java Style
- **✅ Compilation**: Clean compilation with no warnings or errors
- **✅ Tests**: All unit tests passing (currently 72+ tests)
- **✅ Checkstyle**: Code quality standards met (warnings acceptable)

#### **📋 Backend Quality Standards**
```java
// Code quality requirements
{
  "formatting": "Google Java Style (2-space indentation, 100 char lines)",
  "imports": "Organized imports, no unused imports",
  "naming": "Clear, descriptive variable and method names", 
  "documentation": "JavaDoc for all public methods",
  "testing": "Unit tests for all business logic",
  "dependencies": "No circular dependencies, clean architecture"
}
```

---

## 🔄 **Complete Development Workflow**

### **🌟 Step-by-Step Workflow**

#### **1. 🎯 Before Starting Development**
```bash
# Ensure you're on the latest main branch
git checkout main
git pull origin main

# Create feature branch
git checkout -b feature/your-feature-name

# Verify environment
./start-all.sh  # Ensures database, backend, and frontend are running
```

#### **2. 💻 During Development**
```bash
# Frontend development
cd frontend
npm run dev  # Hot reload development server

# Backend development  
cd backend
mvn spring-boot:run  # Hot reload Spring Boot application

# Make your changes...
```

#### **3. 🧪 Before Committing (Critical)**

##### **🔍 Frontend Pre-Commit Checks**
```bash
cd frontend

# Critical quality checks
npm run lint          # Must pass: 0 errors, 0 warnings
npm run type-check    # Must pass: No TypeScript errors
npm run build         # Must pass: Production build successful

# Optional but recommended
npm run test          # Run unit tests (when available)
```

##### **🔍 Backend Pre-Commit Checks**
```bash
cd backend

# Critical quality checks
mvn spotless:apply    # Format code automatically
mvn spotless:check    # Verify formatting compliance
mvn clean compile     # Must pass: No compilation errors
mvn test              # Must pass: All tests green

# Optional but recommended  
mvn checkstyle:check  # Code quality (warnings OK, errors not OK)
```

#### **4. 📝 Commit Changes**
```bash
# Stage changes
git add .

# Check what you're committing
git status
git diff --cached

# Commit with descriptive message
git commit -m "feat: add new dashboard widget

- Implement MetricCard component with trend indicators
- Add responsive design for mobile devices  
- Include loading states and error handling
- Update dashboard layout to use new component

Closes #123"
```

#### **5. 🚀 Before Pushing (Final Verification)**

##### **⚡ Quick Quality Check Script**
```bash
#!/bin/bash
# scripts/pre-push-check.sh

set -e

echo "🔍 Running pre-push quality checks..."

# Frontend checks
echo "📱 Checking frontend quality..."
cd frontend
npm ci --silent
npm run lint
npm run type-check  
npm run build --silent
cd ..

# Backend checks
echo "🔧 Checking backend quality..."
cd backend
mvn spotless:apply -q
mvn spotless:check -q
mvn clean compile -q
mvn test -q
cd ..

echo "✅ All quality checks passed! Ready to push."
```

##### **🎯 Make Script Executable and Use**
```bash
chmod +x scripts/pre-push-check.sh
./scripts/pre-push-check.sh
```

#### **6. 📤 Push to Remote**
```bash
# Push feature branch
git push origin feature/your-feature-name

# Create pull request on GitHub
# Wait for code review and approval
# Merge to main branch
```

---

## 🛠️ **Automated Quality Scripts**

### **📋 Quick Check Script**
Create `scripts/quality-check.sh`:
```bash
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
mvn spotless:apply -q

echo "  📏 Format verification..."
mvn spotless:check -q

echo "  🏗️ Compilation..."
mvn clean compile -q

echo "  🧪 Unit tests..."
mvn test -q

echo "  ✅ Backend checks passed!"
cd ..

echo ""
echo "🎉 All quality checks passed! Ready to push."
echo "📤 Run: git push origin <branch-name>"
```

### **📋 Comprehensive Check Script**
Create `scripts/full-check.sh`:
```bash
#!/bin/bash
# scripts/full-check.sh - Comprehensive quality and integration testing

set -e

echo "🔬 Customer Tracker - Comprehensive Check"
echo "======================================="

# Run quality checks first
./scripts/quality-check.sh

echo ""
echo "🧪 Additional Verification..."

# Integration checks
echo "  🔗 API integration test..."
./start-backend.sh >/dev/null 2>&1 &
BACKEND_PID=$!

# Wait for backend to start
sleep 10

# Test API health
if curl -f -s http://localhost:8080/actuator/health >/dev/null; then
    echo "  ✅ Backend API is responsive"
else
    echo "  ❌ Backend API is not responding"
    kill $BACKEND_PID
    exit 1
fi

# Cleanup
kill $BACKEND_PID

# Frontend integration
echo "  🌐 Frontend build verification..."
cd frontend
npm run build --silent
echo "  ✅ Frontend production build successful"
cd ..

echo ""
echo "🏆 All comprehensive checks passed!"
echo "🚀 Code is production-ready for deployment"
```

### **📋 Git Hooks (Optional)**
Create `.git/hooks/pre-push`:
```bash
#!/bin/bash
# Pre-push hook to ensure code quality

echo "🔍 Running pre-push quality checks..."

# Run our quality check script
if [ -f "./scripts/quality-check.sh" ]; then
    ./scripts/quality-check.sh
else
    echo "⚠️  Quality check script not found, running manual checks..."
    
    # Manual checks
    cd frontend
    npm run lint && npm run type-check && npm run build --silent
    cd ../backend
    mvn spotless:check -q && mvn clean compile -q && mvn test -q
    cd ..
fi

if [ $? -ne 0 ]; then
    echo "❌ Quality checks failed! Push aborted."
    echo "🔧 Fix the issues above and try again."
    exit 1
fi

echo "✅ Quality checks passed! Proceeding with push..."
```

---

## 🎯 **Workflow Best Practices**

### **📝 Commit Message Standards**
```bash
# Format: <type>: <description>

# Types:
feat:     # New feature
fix:      # Bug fix
docs:     # Documentation changes
style:    # Code formatting (no functional changes)
refactor: # Code refactoring (no functional changes)
test:     # Adding or updating tests
chore:    # Build tasks, dependency updates

# Examples:
git commit -m "feat: add customer analytics dashboard

- Implement real-time metrics with Chart.js integration
- Add role-based access control for admin/sales views
- Include interactive status distribution charts
- Add responsive design for mobile devices

Closes #45"

git commit -m "fix: resolve chart centering issue in StatusDistributionChart

- Fix donut chart center text positioning
- Improve legend layout and spacing
- Add proper loading states for charts
- Enhance mobile responsiveness

Fixes #67"
```

### **🔄 Branch Management**
```bash
# Feature development workflow
git checkout main
git pull origin main
git checkout -b feature/dashboard-enhancements

# Work on feature...
# Run quality checks before each commit
./scripts/quality-check.sh

# Commit changes
git add .
git commit -m "feat: enhance dashboard with new widgets"

# Before pushing
./scripts/quality-check.sh
git push origin feature/dashboard-enhancements

# Create pull request on GitHub
# After approval and merge, cleanup
git checkout main
git pull origin main
git branch -d feature/dashboard-enhancements
```

### **🚨 Emergency Hotfix Workflow**
```bash
# For critical production issues
git checkout main
git pull origin main
git checkout -b hotfix/critical-bug-fix

# Make minimal changes to fix issue
# Run quality checks (even for hotfixes)
./scripts/quality-check.sh

# Commit and push
git commit -m "fix: resolve critical login issue

- Fix JWT token validation bug
- Add proper error handling for expired tokens
- Update authentication flow

Fixes #emergency-issue"

./scripts/quality-check.sh
git push origin hotfix/critical-bug-fix

# Create pull request with "hotfix" label
# Fast-track review and merge
```

---

## 🔧 **Development Environment Setup**

### **🎯 One-Time Setup**
```bash
# Clone repository
git clone git@github.com:leojiang/customer-tracker.git
cd customer-tracker

# Make scripts executable
chmod +x scripts/*.sh
chmod +x *.sh

# Setup Git hooks (optional but recommended)
chmod +x .git/hooks/pre-push

# Install dependencies
cd frontend && npm install && cd ..
cd backend && mvn clean compile && cd ..

# Verify setup
./scripts/quality-check.sh
```

### **📋 Daily Development Setup**
```bash
# Start development environment
./start-all.sh

# Verify services are running
curl http://localhost:8080/actuator/health  # Backend
curl http://localhost:3000                  # Frontend

# Create feature branch
git checkout -b feature/your-feature

# Start development...
```

---

## 🚨 **Troubleshooting Quality Issues**

### **📱 Common Frontend Issues**

#### **❌ Linting Errors**
```bash
# Issue: ESLint errors
# Solution: 
npm run lint:fix  # Auto-fix minor issues
npm run lint      # Check remaining issues manually

# Common fixes:
# - Remove unused imports
# - Add missing dependencies to useEffect
# - Remove console.log statements
# - Add curly braces to if statements
```

#### **❌ TypeScript Errors**
```bash
# Issue: Type compilation errors
# Solution:
npm run type-check  # See specific error messages

# Common fixes:
# - Add proper type annotations
# - Use optional chaining (?.) for nullable objects
# - Import missing types
# - Fix interface definitions
```

#### **❌ Build Failures**
```bash
# Issue: Production build fails
# Solution:
npm run build  # See specific build errors

# Common fixes:
# - Fix import paths
# - Resolve circular dependencies
# - Update next.config.js if needed
# - Check environment variables
```

### **🔧 Common Backend Issues**

#### **❌ Formatting Issues**
```bash
# Issue: Spotless formatting violations
# Solution:
mvn spotless:apply  # Auto-format all Java files
mvn spotless:check  # Verify formatting

# Manual fixes needed:
# - Long lines: break into multiple lines
# - Import organization: remove unused imports
# - Method formatting: parameter alignment
```

#### **❌ Compilation Errors**
```bash
# Issue: Maven compilation fails
# Solution:
mvn clean compile  # Clean build

# Common fixes:
# - Fix syntax errors
# - Resolve import issues  
# - Update annotations
# - Fix method signatures
```

#### **❌ Test Failures**
```bash
# Issue: Unit tests failing
# Solution:
mvn test -Dtest=SpecificTestClass  # Run specific test

# Common fixes:
# - Update test data after schema changes
# - Fix mock configurations
# - Update assertions after logic changes
# - Resolve dependency injection issues
```

---

## 🎛️ **Quality Gate Configuration**

### **📊 Quality Standards**

#### **Frontend Quality Gates**
| Check | Requirement | Command | Tolerance |
|-------|------------|---------|-----------|
| **Linting** | 0 errors, 0 warnings | `npm run lint` | ❌ Zero tolerance |
| **TypeScript** | No compilation errors | `npm run type-check` | ❌ Zero tolerance |
| **Build** | Production build success | `npm run build` | ❌ Zero tolerance |
| **Bundle Size** | < 200KB first load JS | `npm run build` | ⚠️ Review if exceeded |

#### **Backend Quality Gates**  
| Check | Requirement | Command | Tolerance |
|-------|------------|---------|-----------|
| **Formatting** | Google Java Style | `mvn spotless:check` | ❌ Zero tolerance |
| **Compilation** | Clean compilation | `mvn clean compile` | ❌ Zero tolerance |
| **Unit Tests** | All tests passing | `mvn test` | ❌ Zero tolerance |
| **Checkstyle** | Code quality standards | `mvn checkstyle:check` | ⚠️ Warnings OK |

### **🚀 Automated Pre-Push Script**
Create `scripts/pre-push.sh`:
```bash
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
```

### **🔄 Make Script Executable**
```bash
chmod +x scripts/pre-push.sh

# Usage before every push:
./scripts/pre-push.sh
git push origin feature-branch-name
```

---

## 📋 **Code Review Checklist**

### **✅ Pull Request Requirements**

#### **📝 PR Description Template**
```markdown
## 📋 Pull Request Summary

### 🎯 What This PR Does
- Brief description of changes
- Business value and impact
- Related issue numbers (#123)

### 🧪 Testing Performed
- [ ] Frontend linting passed (`npm run lint`)
- [ ] TypeScript compilation passed (`npm run type-check`)
- [ ] Production build successful (`npm run build`)
- [ ] Backend formatting applied (`mvn spotless:apply`)
- [ ] Backend tests passed (`mvn test`)
- [ ] Manual testing completed
- [ ] Cross-browser testing (if UI changes)

### 📊 Quality Metrics
- **Bundle Size Change**: +/- X KB
- **Test Coverage**: X% (no decrease allowed)
- **Performance Impact**: None/Positive/Negative (explain)

### 🔍 Review Focus Areas
- [ ] Code quality and maintainability
- [ ] Performance implications
- [ ] Security considerations
- [ ] Documentation updates needed
- [ ] Breaking changes (none expected)

### 📱 Screenshots (for UI changes)
[Include before/after screenshots if applicable]

### 🚀 Deployment Notes
- [ ] Database migration required (Y/N)
- [ ] Environment variables needed (Y/N)
- [ ] Configuration changes required (Y/N)
```

#### **✅ Reviewer Checklist**
- [ ] **Code Quality**: Follows established patterns and conventions
- [ ] **Testing**: Adequate test coverage for new features
- [ ] **Performance**: No negative performance impact
- [ ] **Security**: No security vulnerabilities introduced
- [ ] **Documentation**: README and docs updated if needed
- [ ] **Breaking Changes**: None or properly documented
- [ ] **Dependencies**: No unnecessary new dependencies

---

## 🚨 **Emergency Procedures**

### **🔥 If CI/CD Fails After Push**

#### **📋 Immediate Actions**
1. **🔍 Identify Issue**: Check CI/CD logs for specific errors
2. **🚨 Assess Impact**: Determine if it affects production
3. **🔧 Quick Fix**: Make minimal changes to resolve issue
4. **✅ Verify Locally**: Run quality checks before pushing fix
5. **📤 Push Fix**: Push emergency fix to same branch

#### **🛠️ Common Emergency Fixes**
```bash
# Linting emergency fix
cd frontend
npm run lint:fix
npm run lint  # Verify

# Formatting emergency fix
cd backend  
mvn spotless:apply
mvn spotless:check  # Verify

# Commit and push emergency fix
git add .
git commit -m "fix: emergency CI/CD fix for linting/formatting issues"
git push origin main  # or feature branch
```

### **🔄 Rollback Procedures**
```bash
# If latest commit breaks CI/CD
git revert HEAD
git push origin main

# If multiple commits need rollback
git reset --hard HEAD~3  # Go back 3 commits
git push --force-with-lease origin main  # Use with extreme caution
```

---

## 📊 **Quality Metrics Tracking**

### **📈 Success Metrics**
- **Build Success Rate**: Target 100% (zero failed builds)
- **Code Quality Score**: All quality gates passing
- **Test Coverage**: Maintain current coverage levels
- **Performance**: No regression in build times or bundle sizes

### **📋 Weekly Quality Review**
```bash
# Check quality trends
git log --oneline --since="1 week ago"
git shortlog -s --since="1 week ago"  # Author activity

# Run comprehensive quality check
./scripts/full-check.sh

# Review metrics
npm run build        # Check bundle sizes
mvn test             # Ensure all tests still pass
```

---

## 🎯 **Team Guidelines**

### **👥 For All Developers**
1. **🔍 Always run quality checks** before pushing
2. **📝 Write descriptive commit messages** following conventions
3. **🧪 Include tests** for new features and bug fixes
4. **📚 Update documentation** when adding features
5. **🔄 Keep branches small** and focused on single features

### **👨‍💼 For Code Reviewers**
1. **✅ Verify quality checks** were run by author
2. **🔍 Review code for maintainability** and best practices
3. **🧪 Ensure adequate test coverage** for changes
4. **📊 Check performance impact** of changes
5. **📝 Provide constructive feedback** with specific suggestions

### **🎯 For Team Leads**
1. **📊 Monitor quality metrics** and trends
2. **🎓 Provide training** on quality standards and tools
3. **🔧 Maintain tooling** and update dependencies regularly
4. **📋 Review and update** workflow processes periodically
5. **🏆 Recognize quality contributions** from team members

---

## 📞 **Support & Resources**

### **🔗 Quality Tools Documentation**
- **ESLint**: https://eslint.org/docs/
- **TypeScript**: https://www.typescriptlang.org/docs/
- **Spotless**: https://github.com/diffplug/spotless
- **Checkstyle**: https://checkstyle.sourceforge.io/
- **Maven**: https://maven.apache.org/guides/

### **🆘 Getting Help**
- **🐛 Report Issues**: GitHub Issues with `quality` label
- **💬 Ask Questions**: GitHub Discussions
- **📖 Documentation**: Check `docs/development/` folder
- **👥 Team Chat**: Discuss in team channels

---

**🎯 Following this workflow ensures high code quality, prevents CI/CD failures, and maintains professional development standards!**

**Remember: Quality is everyone's responsibility! 🏆**