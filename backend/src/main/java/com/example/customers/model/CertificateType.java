package com.example.customers.model;

/**
 * Enumeration of certificate types for customers.
 *
 * <p>Provides standardized certificate type options for customer data.
 */
public enum CertificateType {
  ELECTRICIAN("电工", "Electrician"),
  WELDER("焊工", "Welder"),
  EXCAVATOR("挖掘机", "Excavator Operator");

  private final String displayName;
  private final String displayNameEn;

  CertificateType(String displayName, String displayNameEn) {
    this.displayName = displayName;
    this.displayNameEn = displayNameEn;
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getDisplayNameEn() {
    return displayNameEn;
  }

  @Override
  public String toString() {
    return displayName;
  }
}
