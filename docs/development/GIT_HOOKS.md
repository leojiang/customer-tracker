# Git Hooks Setup

> **Automatic quality enforcement with Git hooks**

## 🤔 **Manual vs Automatic Quality Checks**

### **📝 Current Default: Manual Execution**
```bash
# You must remember to run these commands manually:
./scripts/quality-check.sh     # Manual quality verification
git push origin branch-name    # Git push does NOT run any checks
```

**❌ Problems with Manual Approach:**
- Easy to forget running quality checks
- Team members might skip checks when in a hurry
- Inconsistent code quality enforcement
- CI/CD failures when checks are skipped

### **🤖 Recommended: Automatic Execution**
```bash
# Install git hooks once:
./scripts/install-git-hooks.sh

# After installation, checks run automatically:
git push origin branch-name    # Automatically runs quality checks before push
```

**✅ Benefits of Automatic Approach:**
- **Zero-effort quality enforcement** - Never forget quality checks
- **Team consistency** - All team members get same quality gates
- **CI/CD failure prevention** - Catches issues before they reach remote
- **Professional development** - Enterprise-grade quality automation

## 🔧 **Installing Automatic Git Hooks**

### **🚀 Quick Installation**
```bash
# One-time setup for automatic quality checking:
./scripts/install-git-hooks.sh
```

### **📋 What Gets Installed**

#### **🔍 pre-push Hook** - Quality Gates Before Push
```bash
# Installed at: .git/hooks/pre-push
# Runs automatically when you execute: git push

# What it does:
✅ Frontend linting (npm run lint)
✅ TypeScript compilation (npm run type-check)  
✅ Frontend build test (npm run build)
✅ Backend code formatting (mvn spotless:check)
✅ Backend compilation (mvn clean compile)
✅ Backend unit tests (mvn test)

# If any check fails:
❌ Push is aborted with clear error message
🔧 You fix the issue and try push again
```

#### **🎨 pre-commit Hook** - Automatic Formatting
```bash
# Installed at: .git/hooks/pre-commit  
# Runs automatically when you execute: git commit

# What it does:
🎨 Automatically formats Java code (mvn spotless:apply)
📝 Ensures consistent code style before commit
```

### **🔧 Manual Installation** 
If you prefer to install hooks manually:

#### **📤 Pre-Push Hook**
```bash
# Create pre-push hook manually
cat > .git/hooks/pre-push << 'EOF'
#!/bin/bash
echo "🔍 Running automatic pre-push quality checks..."

if ! ./scripts/quality-check.sh; then
    echo "❌ Quality checks failed! Push aborted."
    echo "🔧 Fix issues or use --no-verify to bypass (not recommended)"
    exit 1
fi

echo "✅ Quality checks passed! Proceeding with push..."
EOF

chmod +x .git/hooks/pre-push
```

#### **📝 Pre-Commit Hook**
```bash
# Create pre-commit hook manually
cat > .git/hooks/pre-commit << 'EOF'
#!/bin/bash
echo "🎨 Auto-formatting code before commit..."

# Format backend code
cd backend && mvn spotless:apply -q >/dev/null 2>&1 && cd ..

echo "✅ Formatting completed!"
EOF

chmod +x .git/hooks/pre-commit
```

## 🎯 **How It Works After Installation**

### **📝 Normal Development Workflow**
```bash
# 1. Make changes to code
vim frontend/src/components/MyComponent.tsx

# 2. Stage changes
git add .

# 3. Commit (pre-commit hook runs automatically)
git commit -m "feat: add new component"
# 🎨 Auto-formatting runs automatically

# 4. Push (pre-push hook runs automatically)  
git push origin feature/my-feature
# 🔍 Quality checks run automatically
# ✅ If all pass: push succeeds
# ❌ If any fail: push aborted with error message
```

### **🚨 When Quality Checks Fail**
```bash
# Example of failed push:
$ git push origin main

🔍 Running automatic pre-push quality checks...
📱 Frontend Quality Checks...
  🔍 Linting...
  ❌ ESLint errors found!

❌ Quality checks failed! Push aborted.
🔧 Fix the issues above and try pushing again.

To skip this check (NOT RECOMMENDED):
git push --no-verify origin main
```

### **🔧 Fixing Issues**
```bash
# Fix the linting issues:
npm run lint:fix        # Auto-fix minor issues
npm run lint           # Check remaining issues

# Or fix formatting issues:
mvn spotless:apply     # Auto-format Java code
mvn spotless:check     # Verify formatting

# Try push again (quality checks will run again):
git push origin main
```

## ⚙️ **Advanced Git Hook Configuration**

### **🎛️ Customizing Quality Checks**

#### **Modify Pre-Push Hook**
```bash
# Edit the installed hook:
vim .git/hooks/pre-push

# Add custom checks:
# - Security scanning
# - Performance testing  
# - Documentation validation
# - Custom business rule validation
```

#### **Skip Checks in Emergencies**
```bash
# Emergency push (bypasses all quality checks):
git push --no-verify origin main

# ⚠️ WARNING: Use only for critical hotfixes!
# Always run quality checks manually afterward:
./scripts/quality-check.sh
```

### **🔄 Team-Wide Hook Installation**
```bash
# Add to team onboarding script:
git clone git@github.com:leojiang/customer-tracker.git
cd customer-tracker
chmod +x scripts/*.sh
./scripts/install-git-hooks.sh   # Install quality automation

echo "✅ Development environment ready with automatic quality checks!"
```

### **📋 Hook Management Commands**
```bash
# Check installed hooks
ls -la .git/hooks/

# Remove hooks
rm .git/hooks/pre-push
rm .git/hooks/pre-commit

# Reinstall hooks
./scripts/install-git-hooks.sh

# Test hooks manually
.git/hooks/pre-push         # Test pre-push hook
.git/hooks/pre-commit       # Test pre-commit hook
```

## 🎯 **Recommendations**

### **👥 For Individual Developers**
- **🔧 Install git hooks** for automatic quality enforcement
- **📝 Learn manual commands** for understanding what's being checked
- **🚀 Use --no-verify sparingly** and only for emergencies

### **🏢 For Development Teams**
- **📋 Require git hooks installation** during onboarding
- **🎓 Train team members** on quality standards and tools
- **📊 Monitor quality metrics** and team compliance
- **🔄 Update hooks** as quality standards evolve

### **🎛️ For Project Managers**
- **✅ Enforce automatic quality checks** to prevent CI/CD failures
- **📈 Track quality metrics** and improvement over time
- **🎯 Set team standards** for code quality and compliance
- **🚀 Ensure professional development practices** across the team

---

## 🆘 **Troubleshooting Git Hooks**

### **❌ Common Issues**

#### **Hook Not Executing**
```bash
# Check if hook exists and is executable
ls -la .git/hooks/pre-push

# If not executable:
chmod +x .git/hooks/pre-push

# If doesn't exist:
./scripts/install-git-hooks.sh
```

#### **Quality Check Script Not Found**
```bash
# Ensure scripts are in correct location:
ls -la scripts/quality-check.sh

# If missing, check git status:
git status

# Reinstall if needed:
./scripts/install-git-hooks.sh
```

#### **Permission Issues**
```bash
# Fix script permissions:
chmod +x scripts/*.sh
chmod +x .git/hooks/*
```

### **🔧 Hook Debugging**
```bash
# Test hook manually:
./.git/hooks/pre-push

# Debug with verbose output:
bash -x .git/hooks/pre-push

# Check hook logs:
git push origin main 2>&1 | tee push-log.txt
```

---

## 📊 **Quality Metrics & Monitoring**

### **📈 Success Metrics**
- **Build Success Rate**: 100% (zero CI/CD failures)
- **Code Quality Score**: All quality gates passing
- **Team Compliance**: All pushes pass quality checks
- **Development Velocity**: No delays due to quality issues

### **🔍 Monitoring Quality**
```bash
# Check recent push success
git log --oneline -10

# Verify all commits pass quality
./scripts/quality-check.sh

# Team quality dashboard (example)
git log --pretty=format:"%h %an %s" --since="1 week ago"
```

---

**🎉 With Git hooks installed, your development workflow becomes completely automated and foolproof for maintaining enterprise-grade code quality!**