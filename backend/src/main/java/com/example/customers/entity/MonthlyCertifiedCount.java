package com.example.customers.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity representing monthly certified customer count.
 *
 * <p>Tracks the number of customers who transitioned to CERTIFIED status in each month.
 * The primary key is the month in 'yyyy-MM' format.
 */
@Entity
@Table(name = "monthly_certified_count")
public class MonthlyCertifiedCount {

  @Id
  @Column(name = "month", nullable = false, length = 7)
  private String month;

  @Column(name = "certified_count", nullable = false)
  private Integer certifiedCount;

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

  public String getMonth() {
    return month;
  }

  public void setMonth(String month) {
    this.month = month;
  }

  public Integer getCertifiedCount() {
    return certifiedCount;
  }

  public void setCertifiedCount(Integer certifiedCount) {
    this.certifiedCount = certifiedCount;
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
}
