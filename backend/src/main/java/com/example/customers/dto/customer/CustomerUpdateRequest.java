package com.example.customers.dto.customer;

import com.example.customers.model.CertificateType;
import com.example.customers.model.EducationLevel;
import jakarta.validation.constraints.NotBlank;

public class CustomerUpdateRequest {

  @NotBlank(message = "Name is required")
  private String name;

  private String phone;

  private String certificateIssuer;

  private String businessRequirements;

  private CertificateType certificateType;

  private Integer age;

  private EducationLevel education;

  private String gender;

  private String address;

  @NotBlank(message = "ID card is required")
  private String idCard;

  private String certifiedAt;

  private String customerAgent;

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

  public String getCertifiedAt() {
    return certifiedAt;
  }

  public void setCertifiedAt(String certifiedAt) {
    this.certifiedAt = certifiedAt;
  }

  public String getCustomerAgent() {
    return customerAgent;
  }

  public void setCustomerAgent(String customerAgent) {
    this.customerAgent = customerAgent;
  }
}
