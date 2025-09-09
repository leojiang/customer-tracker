package com.example.customers.model;

/** Enum representing the approval status of a user registration. */
public enum ApprovalStatus {
  PENDING("Pending Approval"),
  APPROVED("Approved"),
  REJECTED("Rejected");

  private final String displayName;

  ApprovalStatus(String displayName) {
    this.displayName = displayName;
  }

  public String getDisplayName() {
    return displayName;
  }
}
