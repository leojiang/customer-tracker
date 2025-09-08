package com.example.customers.repository;

import com.example.customers.model.UserApprovalHistory;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for UserApprovalHistory entity operations.
 *
 * <p>Provides CRUD operations and audit queries for user approval history.
 */
@Repository
public interface UserApprovalHistoryRepository extends JpaRepository<UserApprovalHistory, UUID> {

  /** Get approval history for a specific user ordered by timestamp (newest first). */
  List<UserApprovalHistory> findByUserPhoneOrderByActionTimestampDesc(String userPhone);

  /** Get approval history for actions by a specific admin. */
  List<UserApprovalHistory> findByAdminPhoneOrderByActionTimestampDesc(String adminPhone);

  /** Get recent approval actions since a specific date. */
  @Query(
      "SELECT h FROM UserApprovalHistory h WHERE h.actionTimestamp >= :since "
          + "ORDER BY h.actionTimestamp DESC")
  List<UserApprovalHistory> findRecentActions(@Param("since") ZonedDateTime since);

  /** Get all actions for a specific user-admin combination. */
  List<UserApprovalHistory> findByUserPhoneAndAdminPhoneOrderByActionTimestampDesc(
      String userPhone, String adminPhone);

  /** Count total actions by admin. */
  long countByAdminPhone(String adminPhone);
}