package com.example.customers.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entity representing monthly performance metrics for customer agents.
 *
 * <p>Tracks the number of customers, new customers, conversions, and conversion rate for each
 * customer agent by month. The primary key is composite (customer_agent, month).
 */
@Entity
@Table(name = "monthly_agent_performance")
@IdClass(MonthlyAgentPerformanceId.class)
public class MonthlyAgentPerformance {

  @Id
  @Column(name = "customer_agent", nullable = false, length = 100)
  private String customerAgent;

  @Id
  @Column(name = "month", nullable = false, length = 7)
  private String month;

  @Column(name = "total_customers", nullable = false)
  private Integer totalCustomers;

  @Column(name = "new_customers", nullable = false)
  private Integer newCustomers;

  @Column(name = "conversions", nullable = false)
  private Integer conversions;

  @Column(name = "conversion_rate", nullable = false, precision = 5, scale = 2)
  private BigDecimal conversionRate;

  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
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

  public Integer getTotalCustomers() {
    return totalCustomers;
  }

  public void setTotalCustomers(Integer totalCustomers) {
    this.totalCustomers = totalCustomers;
  }

  public Integer getNewCustomers() {
    return newCustomers;
  }

  public void setNewCustomers(Integer newCustomers) {
    this.newCustomers = newCustomers;
  }

  public Integer getConversions() {
    return conversions;
  }

  public void setConversions(Integer conversions) {
    this.conversions = conversions;
  }

  public BigDecimal getConversionRate() {
    return conversionRate;
  }

  public void setConversionRate(BigDecimal conversionRate) {
    this.conversionRate = conversionRate;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    MonthlyAgentPerformance that = (MonthlyAgentPerformance) o;
    return Objects.equals(customerAgent, that.customerAgent) && Objects.equals(month, that.month);
  }

  @Override
  public int hashCode() {
    return Objects.hash(customerAgent, month);
  }
}
