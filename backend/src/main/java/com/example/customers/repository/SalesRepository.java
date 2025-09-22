package com.example.customers.repository;

import com.example.customers.model.ApprovalStatus;
import com.example.customers.model.Sales;
import com.example.customers.model.SalesRole;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for Sales entity operations.
 *
 * <p>Provides CRUD operations and authentication-related queries for sales users.
 */
@Repository
public interface SalesRepository extends JpaRepository<Sales, UUID> {

  Optional<Sales> findByPhone(String phone);

  boolean existsByPhone(String phone);

  /** Find users by phone number containing the given string (case-insensitive). */
  List<Sales> findByPhoneContainingIgnoreCase(String phone);

  /** Find all sales users with SALES role. */
  List<Sales> findByRole(SalesRole role);

  // ========== User Approval Query Methods ==========

  /** Find users by approval status ordered by creation date. */
  List<Sales> findByApprovalStatusOrderByCreatedAtDesc(ApprovalStatus status);

  /** Find users by approval status with pagination. */
  Page<Sales> findByApprovalStatusOrderByCreatedAtDesc(ApprovalStatus status, Pageable pageable);

  /** Find pending approvals created since a specific date. */
  @Query(
      "SELECT s FROM Sales s WHERE s.approvalStatus = :status "
          + "AND s.createdAt >= :since ORDER BY s.createdAt DESC")
  Page<Sales> findPendingApprovalsSince(
      @Param("status") ApprovalStatus status,
      @Param("since") ZonedDateTime since,
      Pageable pageable);

  /** Count users by approval status. */
  long countByApprovalStatus(ApprovalStatus status);

  /** Find users by multiple approval statuses. */
  @Query(
      "SELECT s FROM Sales s WHERE s.approvalStatus IN :statuses "
          + "ORDER BY s.statusUpdatedAt DESC")
  Page<Sales> findByApprovalStatusIn(
      @Param("statuses") List<ApprovalStatus> statuses, Pageable pageable);

  /** Find approved users by enabled status. */
  @Query(
      "SELECT s FROM Sales s WHERE s.approvalStatus = 'APPROVED' AND s.isEnabled = :enabled "
          + "ORDER BY s.createdAt DESC")
  Page<Sales> findApprovedUsersByEnabledStatus(
      @Param("enabled") Boolean enabled, Pageable pageable);

  // ========== Analytics Query Methods ==========

  /** Get sales leaderboard data with customer and conversion metrics. */
  @Query(
      """
    SELECT s.phone,
           COUNT(DISTINCT c.id) as totalCustomers,
           COUNT(DISTINCT CASE WHEN c.currentStatus = 'BUSINESS_DONE' THEN c.id END) as conversions,
           CASE
             WHEN COUNT(DISTINCT c.id) > 0
             THEN ROUND(COUNT(DISTINCT CASE WHEN c.currentStatus = 'BUSINESS_DONE' THEN c.id END) * 100.0 / COUNT(DISTINCT c.id), 2)
             ELSE 0
           END as conversionRate
    FROM Sales s
    LEFT JOIN Customer c ON s.phone = c.salesPhone
        AND c.createdAt >= :startDate
        AND c.deletedAt IS NULL
    WHERE s.role = 'SALES' AND s.approvalStatus = 'APPROVED'
    GROUP BY s.phone
    ORDER BY
      CASE
        WHEN :metric = 'customers' THEN COUNT(DISTINCT c.id)
        WHEN :metric = 'rate' THEN
          CASE
            WHEN COUNT(DISTINCT c.id) > 0
            THEN COUNT(DISTINCT CASE WHEN c.currentStatus = 'BUSINESS_DONE' THEN c.id END) * 100.0 / COUNT(DISTINCT c.id)
            ELSE 0
          END
        ELSE COUNT(DISTINCT CASE WHEN c.currentStatus = 'BUSINESS_DONE' THEN c.id END)
      END DESC,
      COUNT(DISTINCT c.id) DESC
    """)
  List<Object[]> getSalesLeaderboardData(
      @Param("startDate") ZonedDateTime startDate, @Param("metric") String metric);
}
