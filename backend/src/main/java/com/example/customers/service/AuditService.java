package com.example.customers.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Service for handling audit logging of sensitive operations. Audit logs are written to a separate
 * file for compliance and security monitoring.
 */
@Service
public class AuditService {

  private static final Logger AUDIT_LOGGER = LoggerFactory.getLogger("AUDIT_LOGGER");
  private static final Logger LOGGER = LoggerFactory.getLogger(AuditService.class);

  private final ObjectMapper objectMapper;

  public AuditService(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  /**
   * Log an audit event.
   *
   * @param action The action performed (e.g., CREATE, UPDATE, DELETE)
   * @param entityType The type of entity affected
   * @param entityId The ID of the affected entity (if applicable)
   * @param description Human-readable description of the operation
   * @param parameters Method parameters (optional, for detailed logging)
   */
  public void logAuditEvent(
      String action, String entityType, String entityId, String description, Object parameters) {

    try {
      AuditEvent event = buildAuditEvent(action, entityType, entityId, description, parameters);
      String auditLog = formatAuditEvent(event);

      // Log to dedicated audit logger
      AUDIT_LOGGER.info("AUDIT: {}", auditLog);

      // If critical, also log to main logger with WARN level
      if (event.isCritical()) {
        LOGGER.warn("CRITICAL AUDIT EVENT: {}", auditLog);
      }

    } catch (Exception e) {
      // Don't let audit logging failures break the application
      LOGGER.error(
          "Failed to log audit event: action={}, entityType={}, entityId={}",
          action,
          entityType,
          entityId,
          e);
    }
  }

  /** Build an audit event object from the provided parameters. */
  private AuditEvent buildAuditEvent(
      String action, String entityType, String entityId, String description, Object parameters) {

    AuditEvent event = new AuditEvent();
    event.setEventId(UUID.randomUUID().toString());
    event.setTimestamp(LocalDateTime.now());
    event.setAction(action);
    event.setEntityType(entityType);
    event.setEntityId(entityId);

    // Get user information from security context
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null && authentication.isAuthenticated()) {
      event.setUsername(authentication.getName());
      event.setUserId(
          authentication.getPrincipal() != null ? authentication.getPrincipal().toString() : null);
    }

    // Get correlation ID from MDC
    event.setCorrelationId(MDC.get("correlationId"));

    // Set description and parameters
    event.setDescription(description);
    if (parameters != null) {
      try {
        event.setParameters(objectMapper.writeValueAsString(parameters));
      } catch (JsonProcessingException e) {
        event.setParameters(parameters.toString());
      }
    }

    return event;
  }

  /** Format audit event as JSON string for structured logging. */
  private String formatAuditEvent(AuditEvent event) {
    try {
      return objectMapper.writeValueAsString(event);
    } catch (JsonProcessingException e) {
      LOGGER.error("Failed to format audit event as JSON", e);
      return event.toString();
    }
  }

  /** Internal class representing an audit event. */
  private static class AuditEvent {
    private String eventId;
    private LocalDateTime timestamp;
    private String action;
    private String entityType;
    private String entityId;
    private String username;
    private String userId;
    private String correlationId;
    private String description;
    private String parameters;
    private boolean critical;

    // Getters and setters
    public String getEventId() {
      return eventId;
    }

    public void setEventId(String eventId) {
      this.eventId = eventId;
    }

    public LocalDateTime getTimestamp() {
      return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
      this.timestamp = timestamp;
    }

    public String getAction() {
      return action;
    }

    public void setAction(String action) {
      this.action = action;
    }

    public String getEntityType() {
      return entityType;
    }

    public void setEntityType(String entityType) {
      this.entityType = entityType;
    }

    public String getEntityId() {
      return entityId;
    }

    public void setEntityId(String entityId) {
      this.entityId = entityId;
    }

    public String getUsername() {
      return username;
    }

    public void setUsername(String username) {
      this.username = username;
    }

    public String getUserId() {
      return userId;
    }

    public void setUserId(String userId) {
      this.userId = userId;
    }

    public String getCorrelationId() {
      return correlationId;
    }

    public void setCorrelationId(String correlationId) {
      this.correlationId = correlationId;
    }

    public String getDescription() {
      return description;
    }

    public void setDescription(String description) {
      this.description = description;
    }

    public String getParameters() {
      return parameters;
    }

    public void setParameters(String parameters) {
      this.parameters = parameters;
    }

    public boolean isCritical() {
      return critical;
    }

    public void setCritical(boolean critical) {
      this.critical = critical;
    }

    @Override
    public String toString() {
      return String.format(
          "AuditEvent{eventId='%s', timestamp=%s, action='%s', entityType='%s', entityId='%s', username='%s', userId='%s', correlationId='%s', description='%s'}",
          eventId,
          timestamp,
          action,
          entityType,
          entityId,
          username,
          userId,
          correlationId,
          description);
    }
  }
}
