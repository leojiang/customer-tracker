package com.example.customers.model;

/**
 * Enumeration of education levels for customers.
 *
 * <p>Provides standardized education level options for customer data.
 */
public enum EducationLevel {
  ELEMENTARY("Elementary School"),
  MIDDLE_SCHOOL("Middle School"),
  HIGH_SCHOOL("High School"),
  ASSOCIATE("Associate Degree"),
  BACHELOR("Bachelor's Degree"),
  MASTER("Master's Degree"),
  DOCTORATE("Doctorate/PhD"),
  PROFESSIONAL("Professional Degree"),
  CERTIFICATE("Certificate/Diploma"),
  OTHER("Other");

  private final String displayName;

  EducationLevel(String displayName) {
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
