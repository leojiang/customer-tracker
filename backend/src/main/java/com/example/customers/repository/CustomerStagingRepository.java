package com.example.customers.repository;

import com.example.customers.model.CustomerStaging;
import com.example.customers.model.CustomerStaging.ImportStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Repository for CustomerStaging entity. */
@Repository
public interface CustomerStagingRepository extends JpaRepository<CustomerStaging, UUID> {

  /**
   * Find all staging records by import status.
   *
   * @param status import status
   * @return list of staging records
   */
  List<CustomerStaging> findByImportStatus(ImportStatus status);

  /**
   * Find staging records by import status with pagination.
   *
   * @param status import status (nullable)
   * @param pageable pagination parameters
   * @return page of staging records
   */
  @Query("SELECT s FROM CustomerStaging s WHERE (:status IS NULL OR s.importStatus = :status)")
  Page<CustomerStaging> findByImportStatusOptional(
      @Param("status") ImportStatus status, Pageable pageable);

  /**
   * Count staging records by import status.
   *
   * @param status import status
   * @return count of staging records
   */
  long countByImportStatus(ImportStatus status);

  /**
   * Find staging record by phone number.
   *
   * @param phone phone number
   * @return optional staging record
   */
  Optional<CustomerStaging> findByPhone(String phone);

  /** Delete all staging records (for cleanup after import). */
  void deleteAll();
}
