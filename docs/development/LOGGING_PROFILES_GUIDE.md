# Logging Profiles Quick Reference

## Overview
The application supports multiple logging profiles optimized for different environments and use cases.

## Available Profiles

### 1. **dev** (Default) - Development
**Best for:** Day-to-day development

```bash
--spring.profiles.active=dev
```

**Characteristics:**
- DEBUG level for application code
- WARN level for database operations (minimal overhead)
- Performance monitoring: **DISABLED** (for maximum speed)
- Color-coded console output
- Detailed error messages

**Database Logging:** Minimal (WARN only)

---

### 2. **debug** - Troubleshooting Database Issues
**Best for:** Debugging database problems, analyzing SQL queries

```bash
--spring.profiles.active=debug
```

**Characteristics:**
- DEBUG level for application code
- **VERBOSE SQL logging** (all queries with parameters)
- Hibernate statistics enabled
- Performance monitoring: Enabled with strict thresholds
- Connection pool logging

**Database Logging:** Maximum (all SQL queries + parameters)

**Use when:**
- Investigating slow queries
- Debugging N+1 problems
- Analyzing query patterns
- Optimizing database performance

⚠️ **Warning:** Significant performance overhead. Only use when actively debugging!

---

### 3. **test** - Testing Environment
**Best for:** Running automated tests

```bash
--spring.profiles.active=test
```

**Characteristics:**
- DEBUG level for application code
- WARN level for database operations
- Performance monitoring: Enabled
- Optimized for test execution speed

---

### 4. **staging** - Staging Environment
**Best for:** Pre-production testing

```bash
--spring.profiles.active=staging
```

**Characteristics:**
- INFO level for application
- WARN level for database
- Performance monitoring: Enabled
- Mimics production configuration

---

### 5. **prod** - Production
**Best for:** Production deployment

```bash
--spring.profiles.active=prod
```

**Characteristics:**
- WARN/ERROR level only
- JSON structured logging
- Performance monitoring: Enabled with lenient thresholds
- No SQL logging
- Optimized for performance

**Database Logging:** None (WARN only)

---

## Performance Impact Comparison

| Profile | DB Logging Overhead | Performance Monitoring | Relative Performance |
|---------|---------------------|------------------------|---------------------|
| dev | Minimal (~5%) | Disabled | ⚡⚡⚡⚡⚡ (95%) |
| debug | High (~50-100%) | Enabled | ⚡⚡ (50%) |
| test | Minimal (~5%) | Enabled | ⚡⚡⚡⚡ (90%) |
| staging | Minimal (~3%) | Enabled | ⚡⚡⚡⚡⚡ (97%) |
| prod | None (~1%) | Enabled | ⚡⚡⚡⚡⚡ (99%) |

---

## Quick Switching

### Switch to Debug Mode (when investigating DB issues):
```bash
export SPRING_PROFILE=debug
java -jar customers.jar
```

### Switch back to Normal Development:
```bash
export SPRING_PROFILE=dev
java -jar customers.jar
```

### Disable Performance Monitoring Entirely:
Add to your `application.yml` or profile-specific config:
```yaml
logging:
  performance:
    enabled: false
```

---

## When to Use Each Profile

### Use **dev** for:
- ✅ Daily development work
- ✅ Feature implementation
- ✅ Unit testing locally
- ✅ Code debugging

### Use **debug** ONLY when:
- ⚠️ Investigating database performance issues
- ⚠️ Analyzing SQL query patterns
- ⚠️ Debugging N+1 query problems
- ⚠️ Optimizing repository methods

### Use **test** for:
- ✅ Running automated test suites
- ✅ CI/CD pipelines

### Use **staging** for:
- ✅ Pre-production deployment testing
- ✅ Load testing
- ✅ User acceptance testing

### Use **prod** for:
- ✅ Production deployments
- ✅ Live environments

---

## Customizing Performance Thresholds

You can customize the performance thresholds per environment:

```yaml
# In application-{profile}.yml
logging:
  performance:
    enabled: true
    database-threshold: 2000      # Log DB ops slower than 2 seconds
    service-threshold: 5000       # Log service methods slower than 5 seconds
    controller-threshold: 10000   # Log controllers slower than 10 seconds
```

---

## Temporarily Enabling SQL Logging

If you need to quickly check SQL queries in dev mode without switching profiles:

1. **Option 1: Uncomment the debug lines** in `application-dev.yml`:
```yaml
# org.hibernate.SQL: DEBUG
# org.hibernate.type.descriptor.sql.BasicBinder: TRACE
```

2. **Option 2: Change at runtime** via Actuator:
```bash
curl -X POST http://localhost:8080/actuator/loggers/org.hibernate.SQL \
  -H "Content-Type: application/json" \
  -d '{"configuredLevel": "DEBUG"}'
```

3. **Option 3: Use environment variable**:
```bash
export LOGGING_LEVEL_ORG_HIBERNATE_SQL=DEBUG
java -jar customers.jar
```

Remember to **disable it again** when done debugging!

---

## Monitoring Slow Operations

Even with minimal database logging, slow operations are still logged:

**Example warning:**
```
WARN  SLOW Database OPERATION: Method=CustomerRepository.findAll(), ExecutionTime=1500ms, Threshold=1000ms
```

This helps identify performance issues without the overhead of logging every SQL statement.

---

## Best Practices

1. **Use dev profile by default** - It has minimal overhead
2. **Only use debug profile when actively investigating** - Switch back immediately
3. **Monitor the slow operation warnings** - They catch real issues
4. **Keep performance monitoring enabled** - It has negligible overhead (~1%)
5. **Use correlation IDs** - Track requests through the logs

---

## Troubleshooting

### Database still slow even with minimal logging?

1. Check if you're using the **debug** profile by mistake
2. Verify Hibernate statistics are disabled:
   ```yaml
   spring:
     jpa:
       properties:
         hibernate:
           generate_statistics: false
   ```
3. Check for connection pool issues
4. Review slow query logs for specific problematic queries

### Need more performance?

1. Set performance monitoring to `false`
2. Increase thresholds to reduce log frequency
3. Use async logging (advanced - requires logback config changes)

---

For detailed documentation, see `LOGGING_STRATEGY.md`.
