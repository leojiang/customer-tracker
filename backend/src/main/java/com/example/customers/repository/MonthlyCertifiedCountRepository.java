package com.example.customers.repository;

import com.example.customers.entity.MonthlyCertifiedCount;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

/**
 * Repository for MonthlyCertifiedCount entity.
 *
 * <p>Provides methods for managing monthly certification counts with thread-safe atomic operations.
 */
public interface MonthlyCertifiedCountRepository
    extends JpaRepository<MonthlyCertifiedCount, String> {

  /**
   * Increment certified count for a specific month.
   *
   * <p>If a record doesn't exist for the given month, creates a new one with count = 1. If a record
   * exists, increments the count by 1.
   *
   * <p>This operation is atomic and thread-safe due to the use of INSERT ... ON DUPLICATE KEY
   * UPDATE.
   *
   * @param month The month in 'yyyy-MM' format (e.g., '2024-01')
   */
  @Modifying(flushAutomatically = true)
  @Transactional
  @Query(
      value =
          """
          INSERT INTO monthly_certified_count (month, certified_count, created_at, updated_at)
          VALUES (:month, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
          ON DUPLICATE KEY UPDATE
              certified_count = certified_count + 1,
              updated_at = CURRENT_TIMESTAMP
          """,
      nativeQuery = true)
  void incrementCertifiedCount(@Param("month") String month);

  /**
   * Find monthly certified count by month.
   *
   * @param month The month in 'yyyy-MM' format
   * @return Optional containing the MonthlyCertifiedCount if found
   */
  Optional<MonthlyCertifiedCount> findByMonth(String month);

  /**
   * Find all monthly certified counts ordered by month ascending.
   *
   * @return List of all monthly certified counts ordered by month
   */
  List<MonthlyCertifiedCount> findAllByOrderByMonthAsc();
}
