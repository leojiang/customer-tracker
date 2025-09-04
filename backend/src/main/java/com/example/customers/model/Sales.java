package com.example.customers.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.Where;

import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "sales", uniqueConstraints = {
    @UniqueConstraint(name = "unique_sales_phone", columnNames = "phone")
})
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

  public Sales() {}

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