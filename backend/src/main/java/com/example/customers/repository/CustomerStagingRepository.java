package com.example.customers.repository;

import com.example.customers.model.CustomerStaging;
import com.example.customers.model.CustomerStaging.ImportStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
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
