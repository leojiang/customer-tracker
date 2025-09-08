package com.example.customers.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.ZonedDateTime;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.Where;

/**
 * Sales user entity representing a sales person in the system.
 *
 * <p>Includes authentication credentials and role information for sales users.
 */
@Entity
@Table(
    name = "sales",
    uniqueConstraints = {@UniqueConstraint(name = "unique_sales_phone", columnNames = "phone")})
@SQLDelete(sql = "UPDATE sales SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@Where(clause = "deleted_at IS NULL")
public class Sales {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @NotBlank
  @Column(nullable = false, unique = true)
  private String phone;

  @NotBlank
  @Column(nullable = false)
  private String password;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(name = "role", nullable = false)
  private SalesRole role = SalesRole.SALES;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private ZonedDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private ZonedDateTime updatedAt;

  @Column(name = "deleted_at")
  private ZonedDateTime deletedAt;

  @Enumerated(EnumType.STRING)
  @Column(name = "approval_status")
  private ApprovalStatus approvalStatus = ApprovalStatus.PENDING;

  @Column(name = "approved_by_phone")
  private String approvedByPhone;

  @Column(name = "approved_at")
  private ZonedDateTime approvedAt;

  @Column(name = "rejection_reason")
  private String rejectionReason;

  @Column(name = "status_updated_at")
  private ZonedDateTime statusUpdatedAt = ZonedDateTime.now();

  public Sales() {}

  /**
   * Constructor with phone, password and role.
   *
   * @param phone sales user phone
   * @param password hashed password
   * @param role user role
   */
  public Sales(String phone, String password, SalesRole role) {
    this.phone = phone;
    this.password = password;
    this.role = role;
  }

  // Getters and Setters
  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getPhone() {
    return phone;
  }

  public void setPhone(String phone) {
    this.phone = phone;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public SalesRole getRole() {
    return role;
  }

  public void setRole(SalesRole role) {
    this.role = role;
  }

  public ZonedDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(ZonedDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public ZonedDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(ZonedDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }

  public ZonedDateTime getDeletedAt() {
    return deletedAt;
  }

  public void setDeletedAt(ZonedDateTime deletedAt) {
    this.deletedAt = deletedAt;
  }

  // Soft delete methods
  public boolean isDeleted() {
    return deletedAt != null;
  }

  public void softDelete() {
    this.deletedAt = ZonedDateTime.now();
  }

  public void restore() {
    this.deletedAt = null;
  }

  public ApprovalStatus getApprovalStatus() {
    return approvalStatus;
  }

  public void setApprovalStatus(ApprovalStatus approvalStatus) {
    this.approvalStatus = approvalStatus;
  }

  public String getApprovedByPhone() {
    return approvedByPhone;
  }

  public void setApprovedByPhone(String approvedByPhone) {
    this.approvedByPhone = approvedByPhone;
  }

  public ZonedDateTime getApprovedAt() {
    return approvedAt;
  }

  public void setApprovedAt(ZonedDateTime approvedAt) {
    this.approvedAt = approvedAt;
  }

  public String getRejectionReason() {
    return rejectionReason;
  }

  public void setRejectionReason(String rejectionReason) {
    this.rejectionReason = rejectionReason;
  }

  public ZonedDateTime getStatusUpdatedAt() {
    return statusUpdatedAt;
  }

  public void setStatusUpdatedAt(ZonedDateTime statusUpdatedAt) {
    this.statusUpdatedAt = statusUpdatedAt;
  }

  // Approval status helper methods
  public boolean isApproved() {
    return ApprovalStatus.APPROVED.equals(this.approvalStatus);
  }

  public boolean isPending() {
    return ApprovalStatus.PENDING.equals(this.approvalStatus);
  }

  public boolean isRejected() {
    return ApprovalStatus.REJECTED.equals(this.approvalStatus);
  }

  @Override
  public String toString() {
    return "Sales{"
        + "id="
        + id
        + ", phone='"
        + phone
        + '\''
        + ", role="
        + role
        + ", createdAt="
        + createdAt
        + ", updatedAt="
        + updatedAt
        + ", deletedAt="
        + deletedAt
        + '}';
  }
}
