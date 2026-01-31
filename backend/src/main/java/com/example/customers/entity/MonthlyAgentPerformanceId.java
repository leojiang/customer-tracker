package com.example.customers.entity;

import java.io.Serializable;
import java.util.Objects;

/**
 * Primary key class for MonthlyAgentPerformance entity.
 *
 * <p>Represents the composite key of customer_agent and month.
 */
public class MonthlyAgentPerformanceId implements Serializable {

  private String customerAgent;
  private String month;

  public MonthlyAgentPerformanceId() {}

  public MonthlyAgentPerformanceId(String customerAgent, String month) {
    this.customerAgent = customerAgent;
    this.month = month;
  }

  // Getters and Setters

  public String getCustomerAgent() {
    return customerAgent;
  }

  public void setCustomerAgent(String customerAgent) {
    this.customerAgent = customerAgent;
  }

  public String getMonth() {
    return month;
  }

  public void setMonth(String month) {
    this.month = month;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    MonthlyAgentPerformanceId that = (MonthlyAgentPerformanceId) o;
    return Objects.equals(customerAgent, that.customerAgent) && Objects.equals(month, that.month);
  }

  @Override
  public int hashCode() {
    return Objects.hash(customerAgent, month);
  }
}
