package com.example.customers.model;

/**
 * Enumeration representing sales user roles in the system.
 *
 * <p>Defines access levels for sales users (ADMIN has full access, SALES has limited access).
 */
public enum SalesRole {
  ADMIN("Admin"),
  SALES("Sales");

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
