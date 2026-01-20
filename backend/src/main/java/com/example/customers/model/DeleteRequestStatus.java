package com.example.customers.model;

/**
 * Enumeration representing the status of a customer delete request.
 *
 * <p>Tracks the lifecycle of delete requests from submission to approval/rejection.
 */
public enum DeleteRequestStatus {
  PENDING("Pending"),
  APPROVED("Approved"),
  REJECTED("Rejected");

  private final String displayName;

  DeleteRequestStatus(String displayName) {
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
