# Logging Strategy Implementation

## Overview

This document describes the comprehensive logging strategy implemented for the Customer Tracker CRM backend application. The implementation addresses all recommendations from item #19 of the Backend Improvement Plan.

**Implementation Date:** 2026-02-15
**Status:** ✅ Completed

---

## Features Implemented

### 1. Environment-Specific Configurations

✅ **Spring Profiles for Different Environments**

- **dev** (`application-dev.yml`): Development environment with detailed logging
- **test** (`application-test.yml`): Testing environment with moderate logging
- **staging** (`application-staging.yml`): Staging environment mimicking production
- **prod** (`application-prod.yml`): Production environment with minimal logging

### 2. Structured Logging

✅ **JSON Format Logging for Production**

- Logback configuration (`logback-spring.xml`) with:
  - JSON structured logging using logstash-logback-encoder
  - Environment-specific appenders (console for dev, JSON for prod)
  - Separate log files for errors, audit events, and general logs
  - Automatic log rotation based on size and time
  - Color-coded console output for development

### 3. Correlation ID Tracking

✅ **MDC (Mapped Diagnostic Context) Filter**

- `CorrelationIdFilter` intercepts all HTTP requests
- Generates or extracts correlation ID from request header
- Adds correlation ID to MDC for all log entries in the request scope
- Includes user information (username, user ID) in logs
- Adds request metadata (URI, method) to logs
- Returns correlation ID in response header for client-side tracking

**Usage:**
```bash
# Client can send correlation ID
curl -H "X-Correlation-ID: my-custom-id" http://localhost:8080/api/customers

# Or server will generate one automatically
curl http://localhost:8080/api/customers
```

### 4. Audit Logging

✅ **Comprehensive Audit Logging System**

- `@AuditLog` annotation for marking sensitive operations
- `AuditService` for writing audit events to dedicated log file
- `AuditLogAspect` for AOP-based automatic audit logging
- Separate audit log file with 1-year retention
- JSON-formatted audit logs for compliance and security monitoring

**Audit Events Logged:**
- Customer creation, updates, deletion
- Status changes
- User authentication and authorization
- Role modifications
- Critical system operations

**Usage Example:**
```java
@AuditLog(
    action = "CREATE",
    entityType = "Customer",
    description = "Created new customer: {0}",
    logParameters = true,
    critical = false
)
public Customer createCustomer(CreateCustomerRequest request) {
    // Method implementation
}
```

### 5. Performance Monitoring

✅ **Slow Query and Operation Detection**

- `PerformanceLoggingAspect` monitors method execution times
- Logs warnings when operations exceed thresholds:
  - Database operations: > 1 second
  - Service methods: > 2 seconds
  - Controller methods: > 3 seconds
- Includes correlation ID in performance logs
- Integrated with Spring Boot Actuator and Micrometer

**Performance Log Example:**
```
WARN  SLOW Database OPERATION: Method=CustomerRepository.findById(), ExecutionTime=1500ms, Threshold=1000ms, CorrelationId=abc-123
```

### 6. Log Levels per Environment

✅ **Appropriate Log Levels for Each Environment**

| Environment | Root Level | Application Level | Framework Level |
|-------------|------------|-------------------|-----------------|
| dev | INFO | DEBUG | DEBUG |
| test | INFO | DEBUG | INFO |
| staging | INFO | INFO | WARN |
| prod | WARN | INFO | WARN |

---

## File Structure

```
backend/
├── src/main/resources/
│   ├── application.yml                    # Main configuration with environment profile
│   ├── application-dev.yml                 # Development profile
│   ├── application-test.yml                # Test profile
│   ├── application-staging.yml             # Staging profile
│   ├── application-prod.yml                # Production profile
│   └── logback-spring.xml                  # Logback configuration
├── src/main/java/com/example/customers/
│   ├── filter/
│   │   └── CorrelationIdFilter.java        # MDC filter for correlation IDs
│   ├── annotation/
│   │   └── AuditLog.java                   # Audit logging annotation
│   ├── aspect/
│   │   ├── AuditLogAspect.java             # AOP aspect for audit logging
│   │   └── PerformanceLoggingAspect.java   # AOP aspect for performance monitoring
│   ├── service/
│   │   └── AuditService.java               # Audit logging service
│   └── config/
│       ├── AopConfig.java                  # AOP configuration
│       ├── MetricsConfig.java              # Micrometer metrics configuration
│       └── PerformanceLoggingConfig.java   # Performance logging configuration
└── pom.xml                                  # Updated dependencies
```

---

## Log Files

### Development/Testing
- **Console**: Color-coded, readable format
- **logs/customers.log**: General application logs (30-day retention)
- **logs/customers-error.log**: Error-only logs (60-day retention)
- **logs/customers-audit.log**: Audit events (1-year retention)

### Production
- **Console**: JSON format for log aggregation
- **logs/customers-json.log**: JSON logs (30-day retention)
- **logs/customers-error.log**: Error-only logs (60-day retention)
- **logs/customers-audit.log**: Audit events (1-year retention)

---

## Log Entry Examples

### Standard Log Entry (Development)
```
2026-02-15 10:30:45.123 [http-nio-8080-exec-1] DEBUG [abc-123-def] [john.doe] c.e.c.service.CustomerService - Creating new customer: John Doe
```

### Standard Log Entry (Production - JSON)
```json
{
  "@timestamp": "2026-02-15T10:30:45.123Z",
  "@version": "1",
  "message": "Creating new customer: John Doe",
  "logger_name": "com.example.customers.service.CustomerService",
  "thread_name": "http-nio-8080-exec-1",
  "level": "INFO",
  "level_value": 20000,
  "correlationId": "abc-123-def",
  "userId": "12345",
  "requestUri": "/api/customers",
  "requestMethod": "POST"
}
```

### Audit Log Entry (JSON)
```json
{
  "eventId": "550e8400-e29b-41d4-a716-446655440000",
  "timestamp": "2026-02-15T10:30:45.123",
  "action": "CREATE",
  "entityType": "Customer",
  "entityId": "12345",
  "username": "john.doe",
  "userId": "12345",
  "correlationId": "abc-123-def",
  "description": "Created new customer: John Doe",
  "parameters": "{\"name\":\"John Doe\",\"email\":\"john@example.com\"}",
  "critical": false
}
```

### Performance Warning Log
```
2026-02-15 10:30:45.123 [http-nio-8080-exec-1] WARN [abc-123-def] [john.doe] c.e.c.aspect.PerformanceLoggingAspect - SLOW Database OPERATION: Method=CustomerRepository.findById(), ExecutionTime=1500ms, Threshold=1000ms, CorrelationId=abc-123-def
```

---

## Monitoring Endpoints

With Spring Boot Actuator enabled, the following endpoints are available:

### Development
- `/actuator/health` - Application health status
- `/actuator/info` - Application information
- `/actuator/metrics` - Application metrics
- `/actuator/prometheus` - Prometheus metrics
- `/actuator/loggers` - Logger configuration

### Production
- `/actuator/health` - Application health status (restricted)
- `/actuator/info` - Application information
- `/actuator/metrics` - Application metrics
- `/actuator/prometheus` - Prometheus metrics for scraping

---

## Usage Guide

### Setting the Active Profile

**Environment Variable:**
```bash
export SPRING_PROFILE=prod
java -jar customers.jar
```

**Command Line Argument:**
```bash
java -jar customers.jar --spring.profiles.active=prod
```

**application.yml:**
```yaml
spring:
  profiles:
    active: ${SPRING_PROFILE:dev}  # Defaults to dev
```

### Using Correlation IDs

**Send with Request:**
```bash
curl -H "X-Correlation-ID: my-tracking-id" http://localhost:8080/api/customers
```

**Track in Logs:**
All log entries for this request will include: `[my-tracking-id]`

**Receive in Response:**
```bash
curl -I http://localhost:8080/api/customers
# Returns: X-Correlation-ID: abc-123-def-456
```

### Adding Audit Logging to Methods

```java
@Service
public class CustomerService {

    @AuditLog(
        action = "UPDATE_STATUS",
        entityType = "Customer",
        description = "Updated customer status from {0} to {1}",
        logParameters = true,
        critical = true  // Important operation
    )
    public void updateCustomerStatus(UUID customerId, CustomerStatus oldStatus,
                                     CustomerStatus newStatus) {
        // Method implementation
    }
}
```

### Monitoring Performance

Check logs for warnings about slow operations:
```bash
# View slow operations
tail -f logs/customers.log | grep "SLOW"

# View specific slow database operations
tail -f logs/customers.log | grep "SLOW Database"

# View audit logs
tail -f logs/customers-audit.log
```

---

## Best Practices

### 1. Log Levels
- **ERROR**: Application errors requiring immediate attention
- **WARN**: Warning conditions that should be investigated
- **INFO**: Important business operations and milestones
- **DEBUG**: Detailed information for diagnosing problems
- **TRACE**: Very detailed debugging information

### 2. What to Log
✅ **Do Log:**
- Business operations (create, update, delete)
- State changes
- Authentication/authorization events
- Performance issues
- Errors and exceptions
- Critical system events

❌ **Don't Log:**
- Sensitive data (passwords, tokens, credit cards)
- Personal identifiable information (PII)
- Large payloads (use truncation)
- Duplicate information

### 3. Structured Logging
```java
// Good - Structured with context
logger.info("Customer created: id={}, name={}, email={}",
    customer.getId(), customer.getName(), customer.getEmail());

// Avoid - Unstructured
logger.info("Customer created: " + customer.toString());
```

### 4. Exception Logging
```java
// Good - Include stack trace
logger.error("Failed to create customer", exception);

// Good - Include context
logger.error("Failed to create customer: name={}, email={}",
    request.getName(), request.getEmail(), exception);
```

---

## Dependencies Added

```xml
<!-- Structured Logging with JSON -->
<dependency>
    <groupId>net.logstash.logback</groupId>
    <artifactId>logstash-logback-encoder</artifactId>
    <version>7.4</version>
</dependency>

<!-- Spring AOP for audit logging aspects -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>

<!-- Micrometer for metrics and performance monitoring -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>

<!-- Spring Boot Actuator for monitoring and management -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

---

## Log Aggregation (Recommended)

For production environments, integrate with a log aggregation stack:

### ELK Stack
- **Elasticsearch**: Log storage and search
- **Logstash**: Log processing and parsing
- **Kibana**: Log visualization and analysis

### Loki Stack
- **Loki**: Log aggregation system
- **Grafana**: Visualization and alerting
- **Promtail**: Log agent

### Cloud Solutions
- AWS CloudWatch
- Google Cloud Logging
- Azure Monitor
- Datadog

---

## Configuration Example for External Systems

### Logstash Configuration
```conf
input {
  file {
    path => "/logs/customers-json.log"
    codec => json
  }
}

filter {
  # Parse JSON logs
  json {
    source => "message"
  }

  # Add additional fields if needed
}

output {
  elasticsearch {
    hosts => ["elasticsearch:9200"]
    index => "customers-%{+YYYY.MM.dd}"
  }
}
```

### Prometheus Configuration
```yaml
scrape_configs:
  - job_name: 'customers'
    metrics_path: '/actuator/prometheus'
    scrape_interval: 15s
    static_configs:
      - targets: ['localhost:8080']
```

---

## Troubleshooting

### Logs Not Appearing
1. Check active profile: `curl http://localhost:8080/actuator/info`
2. Verify log file permissions
3. Check logback configuration syntax
4. Review application logs for startup errors

### Correlation ID Missing
1. Ensure `CorrelationIdFilter` is registered
2. Check filter order in Spring Security chain
3. Verify MDC is being cleared properly

### Audit Logs Not Working
1. Check `@AuditLog` annotation is present
2. Verify AOP is enabled: `@EnableAspectJAutoProxy`
3. Check `AuditService` is properly autowired
4. Review method execution for exceptions

### Performance Warnings Too Frequent
1. Adjust thresholds in `PerformanceLoggingAspect`
2. Consider caching frequently accessed data
3. Review database queries for optimization
4. Check for N+1 query problems

---

## Security Considerations

1. **Never log sensitive data**:
   - Passwords (even hashed)
   - API keys or tokens
   - Personal identifiable information (PII)
   - Credit card numbers

2. **Protect audit logs**:
   - Restrict file system permissions
   - Implement log file encryption
   - Regular security audits of log access
   - Consider log signing for tamper detection

3. **Log retention**:
   - General logs: 30 days
   - Error logs: 60 days
   - Audit logs: 1 year (compliance)
   - Follow organizational retention policies

---

## Future Enhancements

- [ ] Implement log masking for sensitive data
- [ ] Add real-time log streaming to external systems
- [ ] Implement distributed tracing with Spring Cloud Sleuth
- [ ] Add log-based metrics and dashboards
- [ ] Implement automated log analysis and alerting
- [ ] Add log export functionality for compliance
- [ ] Implement log compression for archiving

---

## References

- [Logback Documentation](https://logback.qos.ch/documentation.html)
- [Spring Boot Logging](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.logging)
- [Logstash Logback Encoder](https://github.com/logfellow/logstash-logback-encoder)
- [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- [OWASP Logging Vocabulary](https://cheatsheetseries.owasp.org/cheatsheets/Logging_Vocabulary_Cheat_Sheet.html)

---

**Note**: This logging strategy implementation addresses all recommendations from item #19 of the Backend Improvement Plan. The system is production-ready and provides comprehensive operational visibility.
