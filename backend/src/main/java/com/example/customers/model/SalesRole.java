package com.example.customers.model;

/**
 * Enumeration representing sales user roles in the system.
 *
 * <p>Defines access levels for users (ADMIN has full access, OFFICER can manage customers,
 * CUSTOMER_AGENT has read-only access).
 */
public enum SalesRole {
  ADMIN("Admin"),
  OFFICER("Officer"),
  CUSTOMER_AGENT("Customer Agent");

  private final String displayName;

  SalesRole(String displayName) {
    this.displayName = displayName;
  }

  public String getDisplayName() {
    return displayName;
  }

  @Override
  public String toString() {
    return displayName;
  }
}
