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
import java.time.ZonedDateTime;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;

/**
 * CustomerDeleteRequest entity representing a request to delete a customer.
 *
 * <p>Officers can request customer deletion, which must be approved by an Admin.
 * Tracks the full lifecycle of the request including approval/rejection.
 */
@Entity
@Table(name = "customer_delete_requests")
public class CustomerDeleteRequest {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "customer_id", nullable = false)
  private Customer customer;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "requested_by", nullable = false)
  private Sales requestedBy;

  @Enumerated(EnumType.STRING)
  @Column(name = "request_status", nullable = false)
  private DeleteRequestStatus requestStatus = DeleteRequestStatus.PENDING;

  @Column(name = "reason", nullable = false, length = 1000)
  private String reason;

  @Column(name = "customer_name", nullable = false, length = 255)
  private String customerName;

  @Column(name = "customer_phone", nullable = false, length = 20)
  private String customerPhone;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private ZonedDateTime createdAt;

  @Column(name = "reviewed_by")
  private String reviewedBy;

  @Column(name = "reviewed_at")
  private ZonedDateTime reviewedAt;

  @Column(name = "rejection_reason", length = 1000)
  private String rejectionReason;

  /** Default constructor required by JPA. */
  public CustomerDeleteRequest() {}

  /**
   * Constructor for creating a new delete request.
   *
   * @param customer the customer to delete
   * @param requestedBy the sales user requesting deletion
   * @param reason the reason for deletion
   */
  public CustomerDeleteRequest(Customer customer, Sales requestedBy, String reason) {
    this.customer = customer;
    this.requestedBy = requestedBy;
    this.reason = reason;
    this.customerName = customer.getName();
    this.customerPhone = customer.getPhone();
    this.requestStatus = DeleteRequestStatus.PENDING;
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

  public Sales getRequestedBy() {
    return requestedBy;
  }

  public void setRequestedBy(Sales requestedBy) {
    this.requestedBy = requestedBy;
  }

  public DeleteRequestStatus getRequestStatus() {
    return requestStatus;
  }

  public void setRequestStatus(DeleteRequestStatus requestStatus) {
    this.requestStatus = requestStatus;
  }

  public String getReason() {
    return reason;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }

  public String getCustomerName() {
    return customerName;
  }

  public void setCustomerName(String customerName) {
    this.customerName = customerName;
  }

  public String getCustomerPhone() {
    return customerPhone;
  }

  public void setCustomerPhone(String customerPhone) {
    this.customerPhone = customerPhone;
  }

  public ZonedDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(ZonedDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public String getReviewedBy() {
    return reviewedBy;
  }

  public void setReviewedBy(String reviewedBy) {
    this.reviewedBy = reviewedBy;
  }

  public ZonedDateTime getReviewedAt() {
    return reviewedAt;
  }

  public void setReviewedAt(ZonedDateTime reviewedAt) {
    this.reviewedAt = reviewedAt;
  }

  public String getRejectionReason() {
    return rejectionReason;
  }

  public void setRejectionReason(String rejectionReason) {
    this.rejectionReason = rejectionReason;
  }

  // Helper methods
  public boolean isPending() {
    return DeleteRequestStatus.PENDING.equals(this.requestStatus);
  }

  public boolean isApproved() {
    return DeleteRequestStatus.APPROVED.equals(this.requestStatus);
  }

  public boolean isRejected() {
    return DeleteRequestStatus.REJECTED.equals(this.requestStatus);
  }

  /**
   * Approve the delete request.
   *
   * @param adminPhone the phone number of the admin approving the request
   */
  public void approve(String adminPhone) {
    this.requestStatus = DeleteRequestStatus.APPROVED;
    this.reviewedBy = adminPhone;
    this.reviewedAt = ZonedDateTime.now();
  }

  /**
   * Reject the delete request.
   *
   * @param adminPhone the phone number of the admin rejecting the request
   * @param rejectionReason the reason for rejection
   */
  public void reject(String adminPhone, String rejectionReason) {
    this.requestStatus = DeleteRequestStatus.REJECTED;
    this.reviewedBy = adminPhone;
    this.reviewedAt = ZonedDateTime.now();
    this.rejectionReason = rejectionReason;
  }

  @Override
  public String toString() {
    return "CustomerDeleteRequest{"
        + "id="
        + id
        + ", customerId="
        + (customer != null ? customer.getId() : null)
        + ", requestedBy="
        + (requestedBy != null ? requestedBy.getPhone() : null)
        + ", requestStatus="
        + requestStatus
        + ", createdAt="
        + createdAt
        + '}';
  }
}
