package com.example.customers.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.ZonedDateTime;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;

/**
 * Entity representing the history of user approval actions.
 *
 * <p>Tracks all approval, rejection, and reset actions for audit purposes.
 */
@Entity
@Table(name = "user_approval_history")
public class UserApprovalHistory {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "user_phone", nullable = false)
  private String userPhone;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ApprovalAction action;

  @Column(name = "admin_phone", nullable = false)
  private String adminPhone;

  private String reason;

  @CreationTimestamp
  @Column(name = "action_timestamp")
  private ZonedDateTime actionTimestamp;

  public UserApprovalHistory() {}

  /**
   * Constructor for creating approval history records.
   *
   * @param userPhone phone of the user being acted upon
   * @param action the approval action taken
   * @param adminPhone phone of the admin taking the action
   * @param reason reason for the action
   */
  public UserApprovalHistory(
      String userPhone, ApprovalAction action, String adminPhone, String reason) {
    this.userPhone = userPhone;
    this.action = action;
    this.adminPhone = adminPhone;
    this.reason = reason;
  }

  // Getters and Setters
  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getUserPhone() {
    return userPhone;
  }

  public void setUserPhone(String userPhone) {
    this.userPhone = userPhone;
  }

  public ApprovalAction getAction() {
    return action;
  }

  public void setAction(ApprovalAction action) {
    this.action = action;
  }

  public String getAdminPhone() {
    return adminPhone;
  }

  public void setAdminPhone(String adminPhone) {
    this.adminPhone = adminPhone;
  }

  public String getReason() {
    return reason;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }

  public ZonedDateTime getActionTimestamp() {
    return actionTimestamp;
  }

  public void setActionTimestamp(ZonedDateTime actionTimestamp) {
    this.actionTimestamp = actionTimestamp;
  }

  @Override
  public String toString() {
    return "UserApprovalHistory{"
        + "id="
        + id
        + ", userPhone='"
        + userPhone
        + '\''
        + ", action="
        + action
        + ", adminPhone='"
        + adminPhone
        + '\''
        + ", reason='"
        + reason
        + '\''
        + ", actionTimestamp="
        + actionTimestamp
        + '}';
  }
}