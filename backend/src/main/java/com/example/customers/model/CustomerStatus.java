package com.example.customers.model;

/**
 * Enumeration representing customer status values in the system.
 *
 * <p>Defines the various states a customer can be in during the sales process.
 */
public enum CustomerStatus {
  CUSTOMER_CALLED("Customer called"),
  REPLIED_TO_CUSTOMER("Replied to customer"),
  ORDER_PLACED("Order placed"),
  ORDER_CANCELLED("Order cancelled"),
  PRODUCT_DELIVERED("Product delivered"),
  BUSINESS_DONE("Business done"),
  LOST("Lost");

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
