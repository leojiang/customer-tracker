package com.example.customers.repository;

import com.example.customers.model.Customer;
import com.example.customers.model.CustomerStatus;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for Customer entity operations.
 *
 * <p>Provides CRUD operations, custom queries, and search functionality for customers including
 * soft delete support and advanced search capabilities.
 */
@Repository
public interface CustomerRepository
    extends JpaRepository<Customer, UUID>, JpaSpecificationExecutor<Customer> {

  /** Find customer by phone (includes soft-deleted by default due to unique constraint). */
  @Query("SELECT c FROM Customer c WHERE c.phone = :phone")
  Optional<Customer> findByPhoneIncludingDeleted(@Param("phone") String phone);

  /** Find active (non-deleted) customer by phone. */
  Optional<Customer> findByPhone(String phone);

  /** Find active customer by ID. */
  Optional<Customer> findById(UUID id);

  /** Find customer by ID including soft-deleted. */
  @Query("SELECT c FROM Customer c WHERE c.id = :id")
  Optional<Customer> findByIdIncludingDeleted(@Param("id") UUID id);

  /** Soft delete customer. */
  @Modifying
  @Query("UPDATE Customer c SET c.deletedAt = :deletedAt WHERE c.id = :id")
  void softDeleteById(@Param("id") UUID id, @Param("deletedAt") ZonedDateTime deletedAt);

  /** Restore soft-deleted customer. */
  @Modifying
  @Query("UPDATE Customer c SET c.deletedAt = NULL WHERE c.id = :id")
  void restoreById(@Param("id") UUID id);

  /** Count customers by status (active only). */
  long countByCurrentStatus(CustomerStatus status);

  /** Count all customers by status including deleted. */
  @Query("SELECT COUNT(c) FROM Customer c WHERE c.currentStatus = :status")
  long countByCurrentStatusIncludingDeleted(@Param("status") CustomerStatus status);

  /** Find customers updated within last N days. */
  @Query("SELECT c FROM Customer c WHERE c.updatedAt >= :since ORDER BY c.updatedAt DESC")
  Page<Customer> findRecentlyUpdated(@Param("since") ZonedDateTime since, Pageable pageable);
}
