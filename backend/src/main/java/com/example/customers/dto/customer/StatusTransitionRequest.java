package com.example.customers.dto.customer;

import com.example.customers.model.CustomerStatus;

public class StatusTransitionRequest {

  private CustomerStatus toStatus;

  private String reason;

  public CustomerStatus getToStatus() {
    return toStatus;
  }

  public void setToStatus(CustomerStatus toStatus) {
    this.toStatus = toStatus;
  }

  public String getReason() {
    return reason;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }
}
