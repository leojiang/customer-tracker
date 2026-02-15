package com.example.customers.aspect;

import com.example.customers.config.LoggingPerformanceProperties;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

/**
 * Aspect for monitoring and logging slow method executions. Logs warnings when methods take longer
 * than configured thresholds. Can be disabled via configuration.
 */
@Aspect
@Component
public class PerformanceLoggingAspect {

  private static final Logger LOGGER = LoggerFactory.getLogger(PerformanceLoggingAspect.class);
  private final LoggingPerformanceProperties properties;

  public PerformanceLoggingAspect(LoggingPerformanceProperties properties) {
    this.properties = properties;
  }

  /** Pointcut for all repository methods (database operations). */
  @Pointcut("execution(* com.example.customers.repository.*+.*(..))")
  public void repositoryMethods() {}

  /** Pointcut for all service methods. */
  @Pointcut("execution(* com.example.customers.service.*+.*(..))")
  public void serviceMethods() {}

  /** Pointcut for all controller methods. */
  @Pointcut("execution(* com.example.customers.controller.*+.*(..))")
  public void controllerMethods() {}

  /** Around advice for repository methods - detect slow database operations. */
  @Around("repositoryMethods()")
  public Object logRepositoryPerformance(ProceedingJoinPoint joinPoint) throws Throwable {
    if (!properties.isEnabled()) {
      return joinPoint.proceed();
    }
    return measureAndLogPerformance(joinPoint, properties.getDatabaseThreshold(), "Database");
  }

  /** Around advice for service methods - detect slow business logic. */
  @Around("serviceMethods()")
  public Object logServicePerformance(ProceedingJoinPoint joinPoint) throws Throwable {
    if (!properties.isEnabled()) {
      return joinPoint.proceed();
    }
    return measureAndLogPerformance(joinPoint, properties.getServiceThreshold(), "Service");
  }

  /** Around advice for controller methods - detect slow request handling. */
  @Around("controllerMethods()")
  public Object logControllerPerformance(ProceedingJoinPoint joinPoint) throws Throwable {
    if (!properties.isEnabled()) {
      return joinPoint.proceed();
    }
    return measureAndLogPerformance(joinPoint, properties.getControllerThreshold(), "Controller");
  }

  /** Measure method execution time and log if it exceeds threshold. */
  private Object measureAndLogPerformance(
      ProceedingJoinPoint joinPoint, long threshold, String layer) throws Throwable {

    String methodName = joinPoint.getSignature().toShortString();
    long startTime = System.currentTimeMillis();

    try {
      // Execute the method
      Object result = joinPoint.proceed();
      return result;

    } finally {
      long executionTime = System.currentTimeMillis() - startTime;
      String correlationId = MDC.get("correlationId");

      // Log slow operations
      if (executionTime > threshold) {
        LOGGER.warn(
            "SLOW {} OPERATION: Method={}, ExecutionTime={}ms, Threshold={}ms, CorrelationId={}",
            layer,
            methodName,
            executionTime,
            threshold,
            correlationId);
      } else if (LOGGER.isTraceEnabled()) {
        LOGGER.trace(
            "{} operation completed: Method={}, ExecutionTime={}ms, CorrelationId={}",
            layer,
            methodName,
            executionTime,
            correlationId);
      }
    }
  }
}
