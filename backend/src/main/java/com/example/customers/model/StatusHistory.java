package com.example.customers.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.time.ZonedDateTime;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;

/**
 * Status history entity tracking customer status changes.
 *
 * <p>Records all status transitions for audit trail and business intelligence purposes.
 */
@Entity
@Table(name = "status_history")
public class StatusHistory {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "customer_id", nullable = false)
  @NotNull
  private Customer customer;

  @Enumerated(EnumType.STRING)
  @Column(name = "from_status")
  private CustomerStatus fromStatus;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(name = "to_status", nullable = false)
  private CustomerStatus toStatus;

  private String reason;

  @Column(name = "changed_by")
  private String changedBy;

  @CreationTimestamp
  @Column(name = "changed_at", nullable = false, updatable = false)
  private ZonedDateTime changedAt;

  public StatusHistory() {}

  /**
   * Constructor for status history record.
   *
   * @param customer the customer whose status changed
   * @param fromStatus previous status (null for initial status)
   * @param toStatus new status
   * @param reason optional reason for the change
   * @param changedBy name or identifier of the user who made the change
   */
  public StatusHistory(
      Customer customer,
      CustomerStatus fromStatus,
      CustomerStatus toStatus,
      String reason,
      String changedBy) {
    this.customer = customer;
    this.fromStatus = fromStatus;
    this.toStatus = toStatus;
    this.reason = reason;
    this.changedBy = changedBy;
  }

  // Getters and Setters
  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public Customer getCustomer() {
    return customer;
  }

  public void setCustomer(Customer customer) {
    this.customer = customer;
  }

  public CustomerStatus getFromStatus() {
    return fromStatus;
  }

  public void setFromStatus(CustomerStatus fromStatus) {
    this.fromStatus = fromStatus;
  }

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

  public String getChangedBy() {
    return changedBy;
  }

  public void setChangedBy(String changedBy) {
    this.changedBy = changedBy;
  }

  public ZonedDateTime getChangedAt() {
    return changedAt;
  }

  public void setChangedAt(ZonedDateTime changedAt) {
    this.changedAt = changedAt;
  }

  @Override
  public String toString() {
    return "StatusHistory{"
        + "id="
        + id
        + ", customer="
        + (customer != null ? customer.getId() : null)
        + ", fromStatus="
        + fromStatus
        + ", toStatus="
        + toStatus
        + ", reason='"
        + reason
        + '\''
        + ", changedBy='"
        + changedBy
        + '\''
        + ", changedAt="
        + changedAt
        + '}';
  }
}
