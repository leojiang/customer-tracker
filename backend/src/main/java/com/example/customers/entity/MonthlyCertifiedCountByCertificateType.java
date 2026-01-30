package com.example.customers.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entity representing monthly certified customer counts grouped by certificate type.
 *
 * <p>This table tracks how many customers reached CERTIFIED status in a given month,
 * broken down by their certificate type. It provides fast, simplified queries for
 * the Certificate Type Trends chart.
 */
@Entity
@Table(name = "monthly_certified_count_by_certificate_type")
public class MonthlyCertifiedCountByCertificateType implements Serializable {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false, updatable = false)
  private Long id;

  @Column(name = "month", nullable = false, length = 7)
  private String month;

  @Column(name = "certificate_type", nullable = false)
  private String certificateType;

  @Column(name = "certified_count", nullable = false)
  private Integer certifiedCount;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  public MonthlyCertifiedCountByCertificateType() {}

  public MonthlyCertifiedCountByCertificateType(
      String month, String certificateType, Integer certifiedCount) {
    this.month = month;
    this.certificateType = certificateType;
    this.certifiedCount = certifiedCount;
  }

  // Getters and Setters
  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  @jakarta.persistence.PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
  }

  @jakarta.persistence.PreUpdate
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

  public String getCertificateType() {
    return certificateType;
  }

  public void setCertificateType(String certificateType) {
    this.certificateType = certificateType;
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

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    MonthlyCertifiedCountByCertificateType that = (MonthlyCertifiedCountByCertificateType) o;
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public String toString() {
    return "MonthlyCertifiedCountByCertificateType{"
        + "id="
        + id
        + ", month='"
        + month
        + '\''
        + ", certificateType='"
        + certificateType
        + '\''
        + ", certifiedCount="
        + certifiedCount
        + ", createdAt="
        + createdAt
        + ", updatedAt="
        + updatedAt
        + '}';
  }
}
