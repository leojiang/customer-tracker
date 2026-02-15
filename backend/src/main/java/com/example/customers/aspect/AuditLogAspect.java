package com.example.customers.aspect;

import com.example.customers.annotation.AuditLog;
import com.example.customers.service.AuditService;
import java.lang.reflect.Method;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Aspect to handle audit logging for methods annotated with @AuditLog. Intercepts method calls and
 * logs audit events before and after execution.
 */
@Aspect
@Component
public class AuditLogAspect {

  private static final Logger LOGGER = LoggerFactory.getLogger(AuditLogAspect.class);

  private final AuditService auditService;

  public AuditLogAspect(AuditService auditService) {
    this.auditService = auditService;
  }

  /**
   * Around advice for methods annotated with @AuditLog. Logs audit events before method execution
   * and includes execution details.
   */
  @Around("@annotation(com.example.customers.annotation.AuditLog)")
  public Object auditAround(ProceedingJoinPoint joinPoint) throws Throwable {

    MethodSignature signature = (MethodSignature) joinPoint.getSignature();
    Method method = signature.getMethod();
    AuditLog auditAnnotation = method.getAnnotation(AuditLog.class);

    String action = auditAnnotation.action();
    String entityType = auditAnnotation.entityType();
    String description = auditAnnotation.description();
    boolean logParameters = auditAnnotation.logParameters();
    boolean logReturnValue = auditAnnotation.logReturnValue();
    boolean critical = auditAnnotation.critical();

    // Extract entity ID from method parameters if possible
    String entityId = extractEntityId(joinPoint.getArgs());

    // Build description with parameter placeholders
    String finalDescription = buildDescription(description, joinPoint.getArgs());

    // Log parameters if requested
    Object parameters = null;
    if (logParameters) {
      parameters = joinPoint.getArgs();
    }

    LOGGER.debug(
        "Audit log triggered: action={}, entityType={}, description={}",
        action,
        entityType,
        finalDescription);

    // Log audit event before execution
    auditService.logAuditEvent(action, entityType, entityId, finalDescription, parameters);

    long startTime = System.currentTimeMillis();
    Object returnValue = null;
    Exception exception = null;

    try {
      // Execute the actual method
      returnValue = joinPoint.proceed();

      // Update entity ID from return value if not found in parameters
      if (entityId == null && returnValue != null) {
        entityId = extractEntityIdFromReturnValue(returnValue);
      }

      return returnValue;

    } catch (Exception e) {
      exception = e;
      throw e;
    } finally {
      long executionTime = System.currentTimeMillis() - startTime;

      // Log execution completion
      if (exception == null) {
        LOGGER.debug(
            "Audit log completed: action={}, entityType={}, executionTime={}ms",
            action,
            entityType,
            executionTime);

        // Log return value if requested
        if (logReturnValue && returnValue != null) {
          LOGGER.debug("Return value: {}", returnValue);
        }
      } else {
        // Log failed operation
        String errorDescription = finalDescription + " [FAILED: " + exception.getMessage() + "]";
        auditService.logAuditEvent(
            action + "_FAILED", entityType, entityId, errorDescription, parameters);
        LOGGER.error(
            "Audit log failed: action={}, entityType={}, error={}",
            action,
            entityType,
            exception.getMessage());
      }
    }
  }

  /**
   * Extract entity ID from method parameters. Looks for common ID parameter names (id, customerId,
   * userId, etc.)
   */
  private String extractEntityId(Object[] args) {
    if (args == null || args.length == 0) {
      return null;
    }

    for (Object arg : args) {
      if (arg == null) {
        continue;
      }

      // If it's a String or Number, treat it as an ID
      if (arg instanceof String || arg instanceof Number) {
        return arg.toString();
      }

      // If it's an object, try to get ID via getId() method
      try {
        Method getter = arg.getClass().getMethod("getId");
        Object id = getter.invoke(arg);
        if (id != null) {
          return id.toString();
        }
      } catch (Exception e) {
        // Ignore exceptions, continue to next parameter
      }
    }

    return null;
  }

  /** Extract entity ID from return value. */
  private String extractEntityIdFromReturnValue(Object returnValue) {
    if (returnValue == null) {
      return null;
    }

    try {
      Method getter = returnValue.getClass().getMethod("getId");
      Object id = getter.invoke(returnValue);
      if (id != null) {
        return id.toString();
      }
    } catch (Exception e) {
      // Ignore exceptions
    }

    return null;
  }

  /**
   * Build description with parameter placeholders replaced. Supports {0}, {1}, etc. placeholders.
   */
  private String buildDescription(String template, Object[] args) {
    if (template == null || template.isEmpty()) {
      return "";
    }

    String result = template;
    if (args != null && args.length > 0) {
      for (int i = 0; i < args.length; i++) {
        String placeholder = "{" + i + "}";
        String value = args[i] != null ? args[i].toString() : "null";
        result = result.replace(placeholder, value);
      }
    }

    return result;
  }
}
