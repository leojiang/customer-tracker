package com.example.customers.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark methods that require audit logging. Use this on sensitive operations such as
 * create, update, delete, and status changes.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditLog {

  /** Action being performed (e.g., "CREATE", "UPDATE", "DELETE", "STATUS_CHANGE"). */
  String action();

  /** Entity type being affected (e.g., "Customer", "User", "Role"). */
  String entityType();

  /**
   * Description of the operation. Supports placeholders: {0} - first parameter, {1} - second
   * parameter, etc.
   */
  String description() default "";

  /** Whether to log method parameters. */
  boolean logParameters() default true;

  /** Whether to log return value. */
  boolean logReturnValue() default false;

  /**
   * Whether to log the operation as a critical event. Critical events may trigger additional
   * notifications.
   */
  boolean critical() default false;
}
