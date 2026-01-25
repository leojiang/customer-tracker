package com.example.customers.model;

import com.example.customers.validation.IdCard;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import java.time.ZonedDateTime;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;

/**
 * Staging entity for customer import workflow.
 *
 * <p>Stores uploaded customer data temporarily before user confirms import. Allows review and
 * validation before moving to main customers table.
 */
@Entity
@Table(name = "customer_staging")
public class CustomerStaging {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @NotBlank
  @Column(nullable = false)
  private String name;

  @NotBlank
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

  @Enumerated(EnumType.STRING)
  @Column(name = "current_status", nullable = false)
  private CustomerStatus currentStatus = CustomerStatus.NEW;

  @Column(name = "customer_agent")
  private String customerAgent;

  @Column(name = "certified_at")
  private String certifiedAt; // Format: YYYY-MM-DD (e.g., "2024-01-15")

  @Enumerated(EnumType.STRING)
  @Column(name = "import_status", nullable = false)
  private ImportStatus importStatus = ImportStatus.PENDING;

  @Column(name = "validation_message")
  private String validationMessage;

  @Column(name = "changed_fields")
  private String changedFields;

  @Column(name = "excel_row_number")
  private Integer excelRowNumber;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private ZonedDateTime createdAt;

  /** Import status enumeration. */
  public enum ImportStatus {
    PENDING, // Ready to be imported
    VALID, // Valid and will be imported as new
    UPDATE, // Valid and will update existing customer
    DUPLICATE, // Duplicate found (same phone + certificate type)
    INVALID // Validation error
  }

  // Default constructor
  public CustomerStaging() {}

  // Constructor for creating staging record from Customer
  public CustomerStaging(Customer customer) {
    this.name = customer.getName();
    this.phone = customer.getPhone();
    this.certificateIssuer = customer.getCertificateIssuer();
    this.businessRequirements = customer.getBusinessRequirements();
    this.certificateType = customer.getCertificateType();
    this.age = customer.getAge();
    this.education = customer.getEducation();
    this.gender = customer.getGender();
    this.address = customer.getAddress();
    this.idCard = customer.getIdCard();
    this.currentStatus = customer.getCurrentStatus();
    this.customerAgent = customer.getCustomerAgent();
    this.certifiedAt = customer.getCertifiedAt();
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

  public String getCustomerAgent() {
    return customerAgent;
  }

  public void setCustomerAgent(String customerAgent) {
    this.customerAgent = customerAgent;
  }

  public String getCertifiedAt() {
    return certifiedAt;
  }

  public void setCertifiedAt(String certifiedAt) {
    this.certifiedAt = certifiedAt;
  }

  public ImportStatus getImportStatus() {
    return importStatus;
  }

  public void setImportStatus(ImportStatus importStatus) {
    this.importStatus = importStatus;
  }

  public String getChangedFields() {
    return changedFields;
  }

  public void setChangedFields(String changedFields) {
    this.changedFields = changedFields;
  }

  public String getValidationMessage() {
    return validationMessage;
  }

  public void setValidationMessage(String validationMessage) {
    this.validationMessage = validationMessage;
  }

  public Integer getExcelRowNumber() {
    return excelRowNumber;
  }

  public void setExcelRowNumber(Integer excelRowNumber) {
    this.excelRowNumber = excelRowNumber;
  }

  public ZonedDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(ZonedDateTime createdAt) {
    this.createdAt = createdAt;
  }

  /**
   * Convert this staging record to a Customer entity.
   *
   * @return Customer entity
   */
  public Customer toCustomer() {
    Customer customer = new Customer();
    customer.setName(this.name);
    customer.setPhone(this.phone);
    customer.setCertificateIssuer(this.certificateIssuer);
    customer.setBusinessRequirements(this.businessRequirements);
    customer.setCertificateType(this.certificateType);
    customer.setAge(this.age);
    customer.setEducation(this.education);
    customer.setGender(this.gender);
    customer.setAddress(this.address);
    customer.setIdCard(this.idCard);
    customer.setCurrentStatus(this.currentStatus);
    customer.setCustomerAgent(this.customerAgent);
    customer.setCertifiedAt(this.certifiedAt);
    return customer;
  }

  /**
   * Update an existing Customer entity with data from this staging record.
   *
   * @param customer existing customer to update
   */
  public void updateCustomer(Customer customer) {
    customer.setName(this.name);
    customer.setPhone(this.phone);
    customer.setCertificateIssuer(this.certificateIssuer);
    customer.setBusinessRequirements(this.businessRequirements);
    customer.setCertificateType(this.certificateType);
    customer.setAge(this.age);
    customer.setEducation(this.education);
    customer.setGender(this.gender);
    customer.setAddress(this.address);
    customer.setIdCard(this.idCard);
    customer.setCurrentStatus(this.currentStatus);
    customer.setCustomerAgent(this.customerAgent);
    customer.setCertifiedAt(this.certifiedAt);
  }
}
