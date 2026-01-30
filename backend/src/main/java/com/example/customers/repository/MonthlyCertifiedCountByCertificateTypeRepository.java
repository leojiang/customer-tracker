package com.example.customers.repository;

import com.example.customers.entity.MonthlyCertifiedCountByCertificateType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

/**
 * Repository for MonthlyCertifiedCountByCertificateType entity.
 *
 * <p>Provides methods for managing monthly certification counts by certificate type with
 * thread-safe atomic operations.
 */
public interface MonthlyCertifiedCountByCertificateTypeRepository
    extends JpaRepository<MonthlyCertifiedCountByCertificateType, Long> {

  /**
   * Increment certified count for a specific month and certificate type.
   *
   * <p>If a record doesn't exist for the given month and certificate type, creates a new one
   * with count = 1. If a record exists, increments the count by 1.
   *
   * <p>This operation is atomic and thread-safe due to the use of INSERT ... ON DUPLICATE KEY
   * UPDATE.
   *
   * @param month The month in 'yyyy-MM' format (e.g., '2024-01')
   * @param certificateType The certificate type (e.g., 'ISO_9001')
   */
  @Modifying(flushAutomatically = true)
  @Transactional
  @Query(
      value =
          """
          INSERT INTO monthly_certified_count_by_certificate_type
              (month, certificate_type, certified_count, created_at, updated_at)
          VALUES (:month, :certificateType, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
          ON DUPLICATE KEY UPDATE
              certified_count = certified_count + 1,
              updated_at = CURRENT_TIMESTAMP
          """,
      nativeQuery = true)
  void incrementCertifiedCount(
      @Param("month") String month, @Param("certificateType") String certificateType);

  /**
   * Find all records ordered by month ascending, then by certificate type.
   *
   * @return List of all monthly certified counts by certificate type, ordered
   */
  List<MonthlyCertifiedCountByCertificateType> findAllByOrderByMonthAscCertificateTypeAsc();
}
