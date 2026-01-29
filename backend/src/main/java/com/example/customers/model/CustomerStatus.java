package com.example.customers.model;

/**
 * Enumeration representing customer status values in the system.
 *
 * <p>Defines the various states a customer can be in during the certification process.
 *
 * <p>Status transition rules:
 *
 * <ul>
 *   <li>NEW - Initial status for new customers, can transition to any status
 *   <li>NOTIFIED - Customer has been notified for review, cannot return to NEW
 *   <li>ABORTED - Customer abandoned the process, cannot return to NEW
 *   <li>SUBMITTED - Customer has submitted documents, cannot return to NEW
 *   <li>CERTIFIED - Certificate has been issued, cannot return to NEW
 *   <li>CERTIFIED_ELSEWHERE - Customer has been certified at another location or is under review,
 *       cannot return to NEW
 * </ul>
 *
 * <p>Transition Rules:
 *
 * <ul>
 *   <li>NEW → NOTIFIED, ABORTED, SUBMITTED, CERTIFIED, CERTIFIED_ELSEWHERE (all allowed)
 *   <li>NOTIFIED → NOTIFIED, ABORTED, SUBMITTED, CERTIFIED, CERTIFIED_ELSEWHERE (not NEW)
 *   <li>ABORTED → NOTIFIED, ABORTED, SUBMITTED, CERTIFIED, CERTIFIED_ELSEWHERE (not NEW)
 *   <li>SUBMITTED → NOTIFIED, ABORTED, SUBMITTED, CERTIFIED, CERTIFIED_ELSEWHERE (not NEW)
 *   <li>CERTIFIED → NOTIFIED, ABORTED, SUBMITTED, CERTIFIED, CERTIFIED_ELSEWHERE (not NEW)
 *   <li>CERTIFIED_ELSEWHERE → NOTIFIED, ABORTED, SUBMITTED, CERTIFIED, CERTIFIED_ELSEWHERE (not
 *       NEW)
 * </ul>
 */
public enum CustomerStatus {
  NEW("New"),
  NOTIFIED("Notified"),
  ABORTED("Aborted"),
  SUBMITTED("Submitted"),
  CERTIFIED("Certified"),
  CERTIFIED_ELSEWHERE("Certified Elsewhere");

  private final String displayName;

  CustomerStatus(String displayName) {
    this.displayName = displayName;
  }

  public String getDisplayName() {
    return displayName;
  }

  /**
   * Converts display name to CustomerStatus enum.
   *
   * @param displayName the display name to convert
   * @return CustomerStatus enum value
   * @throws IllegalArgumentException if display name is not found
   */
  public static CustomerStatus fromDisplayName(String displayName) {
    for (CustomerStatus status : values()) {
      if (status.displayName.equals(displayName)) {
        return status;
      }
    }
    throw new IllegalArgumentException("No enum constant with display name: " + displayName);
  }

  @Override
  public String toString() {
    return displayName;
  }
}
