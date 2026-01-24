package com.example.customers.model;

import com.example.customers.validation.IdCard;
import com.example.customers.validation.PhoneNumber;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.Where;

/**
 * Customer entity representing a customer in the system.
 *
 * <p>Includes customer information, business details, current status, and audit timestamps.
 * Supports soft delete functionality.
 */
@Entity
@Table(
    name = "customers",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "unique_phone_certificate_type",
          columnNames = {"phone", "certificate_type"})
    })
@SQLDelete(sql = "UPDATE customers SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@Where(clause = "deleted_at IS NULL")
public class Customer {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @NotBlank
  @Column(nullable = false)
  private String name;

  @NotBlank(message = "Phone number is required")
  @PhoneNumber(message = "Phone number must contain only digits")
  @Column(nullable = false)
  private String phone;

  @Column(name = "certificate_issuer")
  private String certificateIssuer;

  @Column(name = "business_requirements")
  private String businessRequirements;

  @Column(name = "certificate_type")
  private CertificateType certificateType;

  private Integer age;

  @Enumerated(EnumType.STRING)
  private EducationLevel education;

  private String gender;

  private String address;

  @IdCard(message = "Identity card must contain only digits and English letters")
  @Column(name = "id_card")
  private String idCard;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(name = "current_status", nullable = false)
  private CustomerStatus currentStatus = CustomerStatus.NEW;

  @Column(name = "sales_phone")
  private String salesPhone;

  @Column(name = "customer_agent")
  private String customerAgent;

  @Column(name = "certified_at")
  private ZonedDateTime certifiedAt;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private ZonedDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private ZonedDateTime updatedAt;

  @Column(name = "deleted_at")
  private ZonedDateTime deletedAt;

  @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @OrderBy("changedAt DESC")
  @JsonIgnore
  private List<StatusHistory> statusHistory = new ArrayList<>();

  public Customer() {}

  public Customer(String name, String phone) {
    this.name = name;
    this.phone = phone;
  }

  /**
   * Constructor with name, phone, and sales phone.
   *
   * @param name customer name
   * @param phone customer phone
   * @param salesPhone sales person phone
   */
  public Customer(String name, String phone, String salesPhone) {
    this.name = name;
    this.phone = phone;
    this.salesPhone = salesPhone;
  }

  // Getters and Setters
  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getPhone() {
    return phone;
  }

  public void setPhone(String phone) {
    this.phone = phone;
  }

  public String getCertificateIssuer() {
    return certificateIssuer;
  }

  public void setCertificateIssuer(String certificateIssuer) {
    this.certificateIssuer = certificateIssuer;
  }

  public String getBusinessRequirements() {
    return businessRequirements;
  }

  public void setBusinessRequirements(String businessRequirements) {
    this.businessRequirements = businessRequirements;
  }

  public CertificateType getCertificateType() {
    return certificateType;
  }

  public void setCertificateType(CertificateType certificateType) {
    this.certificateType = certificateType;
  }

  public Integer getAge() {
    return age;
  }

  public void setAge(Integer age) {
    this.age = age;
  }

  public EducationLevel getEducation() {
    return education;
  }

  public void setEducation(EducationLevel education) {
    this.education = education;
  }

  public String getGender() {
    return gender;
  }

  public void setGender(String gender) {
    this.gender = gender;
  }

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public String getIdCard() {
    return idCard;
  }

  public void setIdCard(String idCard) {
    this.idCard = idCard;
  }

  public CustomerStatus getCurrentStatus() {
    return currentStatus;
  }

  public void setCurrentStatus(CustomerStatus currentStatus) {
    this.currentStatus = currentStatus;
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

  public List<StatusHistory> getStatusHistory() {
    return statusHistory;
  }

  public void setStatusHistory(List<StatusHistory> statusHistory) {
    this.statusHistory = statusHistory;
  }

  public String getSalesPhone() {
    return salesPhone;
  }

  public void setSalesPhone(String salesPhone) {
    this.salesPhone = salesPhone;
  }

  public String getCustomerAgent() {
    return customerAgent;
  }

  public void setCustomerAgent(String customerAgent) {
    this.customerAgent = customerAgent;
  }

  public ZonedDateTime getCertifiedAt() {
    return certifiedAt;
  }

  public void setCertifiedAt(ZonedDateTime certifiedAt) {
    this.certifiedAt = certifiedAt;
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
    return "Customer{"
        + "id="
        + id
        + ", name='"
        + name
        + '\''
        + ", phone='"
        + phone
        + '\''
        + ", certificateIssuer='"
        + certificateIssuer
        + '\''
        + ", currentStatus="
        + currentStatus
        + ", createdAt="
        + createdAt
        + ", updatedAt="
        + updatedAt
        + ", deletedAt="
        + deletedAt
        + '}';
  }
}
