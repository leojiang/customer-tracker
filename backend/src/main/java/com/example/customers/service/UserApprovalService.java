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

  @Autowired
  public UserApprovalService(
      SalesRepository salesRepository, UserApprovalHistoryRepository historyRepository) {
    this.salesRepository = salesRepository;
    this.historyRepository = historyRepository;
  }

  /**
   * Get pending user approvals with pagination.
   *
   * @param pageable pagination parameters
   * @return page of users awaiting approval
   */
  @Transactional(readOnly = true)
  public Page<Sales> getPendingApprovals(Pageable pageable) {
    return salesRepository.findByApprovalStatusOrderByCreatedAtDesc(
        ApprovalStatus.PENDING, pageable);
  }

  /**
   * Get users by approval status with pagination.
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
   * Approve a user registration.
   *
   * @param userPhone phone of the user to approve
   * @param adminPhone phone of the admin performing the approval
   * @param reason reason for approval
   * @return the approved user
   * @throws EntityNotFoundException if user not found
   * @throws IllegalStateException if user is already approved
   */
  public Sales approveUser(String userPhone, String adminPhone, String reason) {
    Sales user =
        salesRepository
            .findByPhone(userPhone)
            .orElseThrow(() -> new EntityNotFoundException("User not found: " + userPhone));

    if (user.isApproved()) {
      throw new IllegalStateException("User is already approved");
    }

    user.setApprovalStatus(ApprovalStatus.APPROVED);
    user.setApprovedByPhone(adminPhone);
    user.setApprovedAt(ZonedDateTime.now());
    user.setStatusUpdatedAt(ZonedDateTime.now());
    user.setRejectionReason(null); // Clear any previous rejection reason

    Sales savedUser = salesRepository.save(user);

    // Record approval history
    UserApprovalHistory history =
        new UserApprovalHistory(userPhone, ApprovalAction.APPROVED, adminPhone, reason);
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
        approveUser(phone, adminPhone, reason);
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
