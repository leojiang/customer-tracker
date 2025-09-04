package com.example.customers.model;

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