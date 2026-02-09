package com.example.customers.service;

import com.example.customers.model.ApprovalAction;
import com.example.customers.model.ApprovalStatus;
import com.example.customers.model.Sales;
import com.example.customers.model.UserApprovalHistory;
import com.example.customers.repository.SalesRepository;
import com.example.customers.repository.UserApprovalHistoryRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.ZonedDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service class for user approval operations.
 *
 * <p>Handles approval, rejection, and bulk operations for user registration approvals.
 */
@Service
@Transactional
public class UserApprovalService {

  private static final Logger log = LoggerFactory.getLogger(UserApprovalService.class);

  private final SalesRepository salesRepository;
  private final UserApprovalHistoryRepository historyRepository;
  private final PasswordEncoder passwordEncoder;

  @Autowired
  public UserApprovalService(
      SalesRepository salesRepository,
      UserApprovalHistoryRepository historyRepository,
      PasswordEncoder passwordEncoder) {
    this.salesRepository = salesRepository;
    this.historyRepository = historyRepository;
    this.passwordEncoder = passwordEncoder;
  }

  /**
   * Get pending user approvals with pagination.
   *
   * @param status the approval status to filter by
   * @param pageable pagination parameters
   * @return page of users with the specified status
   */
  @Transactional(readOnly = true)
  public Page<Sales> getUsersByApprovalStatus(ApprovalStatus status, Pageable pageable) {
    return salesRepository.findByApprovalStatusOrderByCreatedAtDesc(status, pageable);
  }

  /**
   * Get all users with pagination for management purposes.
   *
   * @param pageable pagination parameters
   * @return page of all users
   */
  @Transactional(readOnly = true)
  public Page<Sales> getAllUsers(Pageable pageable) {
    return salesRepository.findAll(pageable);
  }

  /**
   * Get approved users by enabled status with pagination.
   *
   * @param enabled whether to get enabled or disabled users
   * @param pageable pagination parameters
   * @return page of approved users filtered by enabled status
   */
  @Transactional(readOnly = true)
  public Page<Sales> getApprovedUsersByEnabledStatus(Boolean enabled, Pageable pageable) {
    return salesRepository.findApprovedUsersByEnabledStatus(enabled, pageable);
  }

  /**
   * Approve a user registration.
   *
   * @param userPhone phone of the user to approve
   * @param adminPhone phone of the admin performing the approval
   * @param reason reason for approval
   * @param requestedRole optional role to assign during approval
   * @return the approved user
   * @throws EntityNotFoundException if user not found
   * @throws IllegalStateException if user is already approved
   */
  public Sales approveUser(String userPhone, String adminPhone, String reason, com.example.customers.model.SalesRole requestedRole) {
    Sales user =
        salesRepository
            .findByPhone(userPhone)
            .orElseThrow(() -> new EntityNotFoundException("User not found: " + userPhone));

    if (user.isApproved()) {
      throw new IllegalStateException("User is already approved");
    }

    // Validate and apply role change if requested
    com.example.customers.model.SalesRole originalRole = null;
    if (requestedRole != null) {
      originalRole = user.getRole();

      // Cannot change from other roles to ADMIN role (security restriction)
      if (requestedRole == com.example.customers.model.SalesRole.ADMIN && originalRole != com.example.customers.model.SalesRole.ADMIN) {
        throw new IllegalStateException("Cannot change user role to ADMIN");
      }

      // Apply role change
      user.setRole(requestedRole);
      log.info("User {} role changed from {} to {} by admin {}", userPhone, originalRole, requestedRole, adminPhone);
    }

    user.setApprovalStatus(ApprovalStatus.APPROVED);
    user.setApprovedByPhone(adminPhone);
    user.setApprovedAt(ZonedDateTime.now());
    user.setStatusUpdatedAt(ZonedDateTime.now());
    user.setRejectionReason(null); // Clear any previous rejection reason

    Sales savedUser = salesRepository.save(user);

    // Record approval history with role change info
    String historyReason = reason;
    if (requestedRole != null && !requestedRole.equals(originalRole)) {
      historyReason = reason + " [Role: " + requestedRole + "]";
    }

    UserApprovalHistory history =
        new UserApprovalHistory(userPhone, ApprovalAction.APPROVED, adminPhone, historyReason);
    historyRepository.save(history);

    log.info("User {} approved by admin {}", userPhone, adminPhone);
    return savedUser;
  }

  /**
   * Reject a user registration.
   *
   * @param userPhone phone of the user to reject
   * @param adminPhone phone of the admin performing the rejection
   * @param reason reason for rejection
   * @return the rejected user
   * @throws EntityNotFoundException if user not found
   */
  public Sales rejectUser(String userPhone, String adminPhone, String reason) {
    Sales user =
        salesRepository
            .findByPhone(userPhone)
            .orElseThrow(() -> new EntityNotFoundException("User not found: " + userPhone));

    user.setApprovalStatus(ApprovalStatus.REJECTED);
    user.setApprovedByPhone(adminPhone);
    user.setApprovedAt(null);
    user.setRejectionReason(reason);
    user.setStatusUpdatedAt(ZonedDateTime.now());

    Sales savedUser = salesRepository.save(user);

    // Record rejection history
    UserApprovalHistory history =
        new UserApprovalHistory(userPhone, ApprovalAction.REJECTED, adminPhone, reason);
    historyRepository.save(history);

    log.info("User {} rejected by admin {}", userPhone, adminPhone);
    return savedUser;
  }

  /**
   * Reset a user's approval status back to pending.
   *
   * @param userPhone phone of the user to reset
   * @param adminPhone phone of the admin performing the reset
   * @param reason reason for reset
   * @return the reset user
   * @throws EntityNotFoundException if user not found
   */
  public Sales resetUserStatus(String userPhone, String adminPhone, String reason) {
    Sales user =
        salesRepository
            .findByPhone(userPhone)
            .orElseThrow(() -> new EntityNotFoundException("User not found: " + userPhone));

    user.setApprovalStatus(ApprovalStatus.PENDING);
    user.setApprovedByPhone(null);
    user.setApprovedAt(null);
    user.setRejectionReason(null);
    user.setStatusUpdatedAt(ZonedDateTime.now());

    Sales savedUser = salesRepository.save(user);

    // Record reset history
    UserApprovalHistory history =
        new UserApprovalHistory(userPhone, ApprovalAction.RESET, adminPhone, reason);
    historyRepository.save(history);

    log.info("User {} status reset to PENDING by admin {}", userPhone, adminPhone);
    return savedUser;
  }

  /**
   * Bulk approve multiple users.
   *
   * @param userPhones list of user phones to approve
   * @param adminPhone phone of the admin performing the approvals
   * @param reason reason for bulk approval
   * @return number of successfully approved users
   */
  public int bulkApprove(List<String> userPhones, String adminPhone, String reason) {
    int approvedCount = 0;
    for (String phone : userPhones) {
      try {
        approveUser(phone, adminPhone, reason, null);
        approvedCount++;
      } catch (Exception e) {
        log.warn("Failed to approve user {}: {}", phone, e.getMessage());
      }
    }
    log.info(
        "Bulk approved {} out of {} users by admin {}",
        approvedCount,
        userPhones.size(),
        adminPhone);
    return approvedCount;
  }

  /**
   * Bulk reject multiple users.
   *
   * @param userPhones list of user phones to reject
   * @param adminPhone phone of the admin performing the rejections
   * @param reason reason for bulk rejection
   * @return number of successfully rejected users
   */
  public int bulkReject(List<String> userPhones, String adminPhone, String reason) {
    int rejectedCount = 0;
    for (String phone : userPhones) {
      try {
        rejectUser(phone, adminPhone, reason);
        rejectedCount++;
      } catch (Exception e) {
        log.warn("Failed to reject user {}: {}", phone, e.getMessage());
      }
    }
    log.info(
        "Bulk rejected {} out of {} users by admin {}",
        rejectedCount,
        userPhones.size(),
        adminPhone);
    return rejectedCount;
  }

  /**
   * Get approval history for a specific user.
   *
   * @param userPhone phone of the user
   * @return list of approval history records
   */
  @Transactional(readOnly = true)
  public List<UserApprovalHistory> getUserApprovalHistory(String userPhone) {
    return historyRepository.findByUserPhoneOrderByActionTimestampDesc(userPhone);
  }

  /**
   * Get approval statistics.
   *
   * @return statistics about user approvals
   */
  @Transactional(readOnly = true)
  public ApprovalStatistics getApprovalStatistics() {
    long pendingCount = salesRepository.countByApprovalStatus(ApprovalStatus.PENDING);
    long approvedCount = salesRepository.countByApprovalStatus(ApprovalStatus.APPROVED);
    long rejectedCount = salesRepository.countByApprovalStatus(ApprovalStatus.REJECTED);

    ZonedDateTime weekAgo = ZonedDateTime.now().minusDays(7);
    List<UserApprovalHistory> recentActions = historyRepository.findRecentActions(weekAgo);

    return new ApprovalStatistics(pendingCount, approvedCount, rejectedCount, recentActions.size());
  }

  /**
   * Get recent approval activity.
   *
   * @param days number of days to look back
   * @return list of recent approval actions
   */
  @Transactional(readOnly = true)
  public List<UserApprovalHistory> getRecentActivity(int days) {
    ZonedDateTime since = ZonedDateTime.now().minusDays(days);
    return historyRepository.findRecentActions(since);
  }

  /**
   * Reset user password (admin only).
   *
   * <p>Generates a secure temporary password and resets the user's password.
   *
   * @param userPhone phone of the user whose password to reset
   * @return the generated temporary password
   * @throws EntityNotFoundException if user not found
   */
  public String resetUserPassword(String userPhone) {
    Sales user =
        salesRepository
            .findByPhone(userPhone)
            .orElseThrow(() -> new EntityNotFoundException("User not found: " + userPhone));

    // Generate a secure temporary password (12 characters with mixed case, numbers, and special
    // chars)
    String temporaryPassword = generateSecurePassword();

    // Hash and set the new password
    String hashedPassword = passwordEncoder.encode(temporaryPassword);
    user.setPassword(hashedPassword);

    // Set flag to force password change on next login
    user.setMustChangePassword(true);

    // Save the updated user
    salesRepository.save(user);

    log.info("Password reset for user {} by admin, must change password on next login", userPhone);

    return temporaryPassword;
  }

  /**
   * Generate a secure random password.
   *
   * @return secure random password with at least 12 characters
   */
  private String generateSecurePassword() {
    String upperCase = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    String lowerCase = "abcdefghijklmnopqrstuvwxyz";
    String digits = "0123456789";
    String specialChars = "!@#$%^&*";

    String allChars = upperCase + lowerCase + digits + specialChars;

    StringBuilder password = new StringBuilder();

    // Ensure at least one character from each category
    password.append(upperCase.charAt((int) (Math.random() * upperCase.length())));
    password.append(lowerCase.charAt((int) (Math.random() * lowerCase.length())));
    password.append(digits.charAt((int) (Math.random() * digits.length())));
    password.append(specialChars.charAt((int) (Math.random() * specialChars.length())));

    // Fill the rest with random characters (total 12 characters)
    for (int i = 4; i < 12; i++) {
      password.append(allChars.charAt((int) (Math.random() * allChars.length())));
    }

    // Shuffle the characters to avoid predictable patterns
    return shuffleString(password.toString());
  }

  /**
   * Shuffle a string randomly.
   *
   * @param input string to shuffle
   * @return shuffled string
   */
  private String shuffleString(String input) {
    char[] characters = input.toCharArray();
    for (int i = 0; i < characters.length; i++) {
      int randomIndex = (int) (Math.random() * characters.length);
      char temp = characters[i];
      characters[i] = characters[randomIndex];
      characters[randomIndex] = temp;
    }
    return new String(characters);
  }

  /**
   * Enable a user account.
   *
   * @param userPhone phone of the user to enable
   * @param adminPhone phone of the admin performing the action
   * @param reason reason for enabling
   * @return the enabled user
   * @throws EntityNotFoundException if user not found
   * @throws IllegalStateException if user is not approved
   */
  public Sales enableUser(String userPhone, String adminPhone, String reason) {
    Sales user =
        salesRepository
            .findByPhone(userPhone)
            .orElseThrow(() -> new EntityNotFoundException("User not found: " + userPhone));

    if (!user.isApproved()) {
      throw new IllegalStateException("Only approved users can be enabled");
    }

    if (user.isEnabled()) {
      throw new IllegalStateException("User is already enabled");
    }

    user.enable();
    Sales savedUser = salesRepository.save(user);

    // Record enable history
    UserApprovalHistory history =
        new UserApprovalHistory(userPhone, ApprovalAction.ENABLED, adminPhone, reason);
    historyRepository.save(history);

    log.info("User {} enabled by admin {}", userPhone, adminPhone);
    return savedUser;
  }

  /**
   * Disable a user account.
   *
   * @param userPhone phone of the user to disable
   * @param adminPhone phone of the admin performing the action
   * @param reason reason for disabling
   * @return the disabled user
   * @throws EntityNotFoundException if user not found
   * @throws IllegalStateException if user is not approved
   */
  public Sales disableUser(String userPhone, String adminPhone, String reason) {
    Sales user =
        salesRepository
            .findByPhone(userPhone)
            .orElseThrow(() -> new EntityNotFoundException("User not found: " + userPhone));

    if (!user.isApproved()) {
      throw new IllegalStateException("Only approved users can be disabled");
    }

    if (user.isDisabled()) {
      throw new IllegalStateException("User is already disabled");
    }

    // Prevent users from disabling themselves
    if (userPhone.equals(adminPhone)) {
      throw new IllegalStateException("Cannot disable your own account");
    }

    user.disable(adminPhone, reason);
    Sales savedUser = salesRepository.save(user);

    // Record disable history
    UserApprovalHistory history =
        new UserApprovalHistory(userPhone, ApprovalAction.DISABLED, adminPhone, reason);
    historyRepository.save(history);

    log.info("User {} disabled by admin {}", userPhone, adminPhone);
    return savedUser;
  }

  /**
   * Bulk enable multiple users.
   *
   * @param userPhones list of user phones to enable
   * @param adminPhone phone of the admin performing the actions
   * @param reason reason for bulk enable
   * @return number of successfully enabled users
   */
  public int bulkEnable(List<String> userPhones, String adminPhone, String reason) {
    int enabledCount = 0;
    for (String phone : userPhones) {
      try {
        enableUser(phone, adminPhone, reason);
        enabledCount++;
      } catch (Exception e) {
        log.warn("Failed to enable user {}: {}", phone, e.getMessage());
      }
    }
    log.info(
        "Bulk enabled {} out of {} users by admin {}", enabledCount, userPhones.size(), adminPhone);
    return enabledCount;
  }

  /**
   * Bulk disable multiple users.
   *
   * @param userPhones list of user phones to disable
   * @param adminPhone phone of the admin performing the actions
   * @param reason reason for bulk disable
   * @return number of successfully disabled users
   */
  public int bulkDisable(List<String> userPhones, String adminPhone, String reason) {
    int disabledCount = 0;
    for (String phone : userPhones) {
      try {
        // Skip if trying to disable self in bulk operation
        if (!phone.equals(adminPhone)) {
          disableUser(phone, adminPhone, reason);
          disabledCount++;
        } else {
          log.warn("Skipping self-disable in bulk operation for user {}", adminPhone);
        }
      } catch (Exception e) {
        log.warn("Failed to disable user {}: {}", phone, e.getMessage());
      }
    }
    log.info(
        "Bulk disabled {} out of {} users by admin {}",
        disabledCount,
        userPhones.size(),
        adminPhone);
    return disabledCount;
  }

  /** DTO class for approval statistics. */
  public static class ApprovalStatistics {
    private final long pendingCount;
    private final long approvedCount;
    private final long rejectedCount;
    private final int recentActivityCount;

    public ApprovalStatistics(
        long pendingCount, long approvedCount, long rejectedCount, int recentActivityCount) {
      this.pendingCount = pendingCount;
      this.approvedCount = approvedCount;
      this.rejectedCount = rejectedCount;
      this.recentActivityCount = recentActivityCount;
    }

    public long getPendingCount() {
      return pendingCount;
    }

    public long getApprovedCount() {
      return approvedCount;
    }

    public long getRejectedCount() {
      return rejectedCount;
    }

    public int getRecentActivityCount() {
      return recentActivityCount;
    }

    public double getApprovalRate() {
      long total = approvedCount + rejectedCount;
      return total > 0 ? (double) approvedCount / total * 100 : 0.0;
    }
  }
}
