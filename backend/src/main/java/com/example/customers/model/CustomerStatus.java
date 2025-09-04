package com.example.customers.model;

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
