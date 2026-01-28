package com.example.customers.repository;

import com.example.customers.model.Customer;
import com.example.customers.model.CustomerStatus;
import java.time.ZonedDateTime;
import java.util.List;
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

  /** Find all customers by phone (supports multiple certificates per phone). */
  List<Customer> findAllByPhone(String phone);

  /** Find customer by name, phone, and certificate type. */
  Optional<Customer> findByNameAndPhoneAndCertificateType(
      String name, String phone, com.example.customers.model.CertificateType certificateType);

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

  // ========== Analytics Query Methods ==========

  /** Count total active customers. */
  @Query("SELECT COUNT(c) FROM Customer c WHERE c.deletedAt IS NULL")
  long countTotalActiveCustomers();

  /** Count total active customers for specific sales person. */
  @Query("SELECT COUNT(c) FROM Customer c WHERE c.deletedAt IS NULL AND c.salesPhone = :salesPhone")
  long countTotalActiveCustomersBySales(@Param("salesPhone") String salesPhone);

  /** Count new customers in date range based on certifiedAt. */
  @Query(
      "SELECT COUNT(c) FROM Customer c WHERE c.deletedAt IS NULL AND c.certifiedAt IS NOT NULL AND c.certifiedAt BETWEEN :startDate AND :endDate")
  long countNewCustomersInPeriod(
      @Param("startDate") String startDate, @Param("endDate") String endDate);

  /** Count new customers in date range for specific sales person based on certifiedAt. */
  @Query(
      "SELECT COUNT(c) FROM Customer c WHERE c.deletedAt IS NULL AND c.salesPhone = :salesPhone AND c.certifiedAt IS NOT NULL AND c.certifiedAt BETWEEN :startDate AND :endDate")
  long countNewCustomersInPeriodBySales(
      @Param("salesPhone") String salesPhone,
      @Param("startDate") String startDate,
      @Param("endDate") String endDate);

  /** Count customers by status. */
  @Query(
      "SELECT c.currentStatus, COUNT(c) FROM Customer c WHERE c.deletedAt IS NULL GROUP BY c.currentStatus")
  List<Object[]> countCustomersByStatus();

  /** Count customers by status for specific sales person. */
  @Query(
      "SELECT c.currentStatus, COUNT(c) FROM Customer c WHERE c.deletedAt IS NULL AND c.salesPhone = :salesPhone GROUP BY c.currentStatus")
  List<Object[]> countCustomersByStatusForSales(@Param("salesPhone") String salesPhone);

  /** Count conversions (BUSINESS_DONE status). */
  long countByCurrentStatusAndDeletedAtIsNull(CustomerStatus currentStatus);

  /** Count conversions for specific sales person. */
  long countByCurrentStatusAndSalesPhoneAndDeletedAtIsNull(
      CustomerStatus currentStatus, String salesPhone);

  /** Count conversions in date range. */
  @Query(
      "SELECT COUNT(c) FROM Customer c WHERE c.deletedAt IS NULL AND c.currentStatus = :status AND c.createdAt BETWEEN :startDate AND :endDate")
  long countConversionsInPeriod(
      @Param("status") CustomerStatus status,
      @Param("startDate") ZonedDateTime startDate,
      @Param("endDate") ZonedDateTime endDate);

  /** Count conversions in date range for specific sales person. */
  @Query(
      "SELECT COUNT(c) FROM Customer c WHERE c.deletedAt IS NULL AND c.currentStatus = :status AND c.salesPhone = :salesPhone AND c.createdAt BETWEEN :startDate AND :endDate")
  long countConversionsInPeriodBySales(
      @Param("status") CustomerStatus status,
      @Param("salesPhone") String salesPhone,
      @Param("startDate") ZonedDateTime startDate,
      @Param("endDate") ZonedDateTime endDate);

  /** Get customer acquisition trends by date. */
  @Query(
      "SELECT c.certifiedAt as date, COUNT(c) as count FROM Customer c WHERE c.deletedAt IS NULL AND c.certifiedAt IS NOT NULL AND c.certifiedAt BETWEEN :startDate AND :endDate GROUP BY c.certifiedAt ORDER BY c.certifiedAt")
  List<Object[]> getCustomerTrendsByDate(
      @Param("startDate") String startDate, @Param("endDate") String endDate);

  /** Get customer acquisition trends by month. */
  @Query(
      "SELECT DATE_FORMAT(c.certifiedAt, '%Y-%m') as month, COUNT(c) as count FROM Customer c WHERE c.deletedAt IS NULL AND c.certifiedAt IS NOT NULL AND c.certifiedAt BETWEEN :startDate AND :endDate GROUP BY DATE_FORMAT(c.certifiedAt, '%Y-%m') ORDER BY month")
  List<Object[]> getCustomerTrendsByMonth(
      @Param("startDate") String startDate, @Param("endDate") String endDate);

  /** Get customer certification trends by date for specific sales person. */
  @Query(
      "SELECT c.certifiedAt as date, COUNT(c) as count FROM Customer c WHERE c.deletedAt IS NULL AND c.salesPhone = :salesPhone AND c.certifiedAt IS NOT NULL AND c.certifiedAt BETWEEN :startDate AND :endDate GROUP BY c.certifiedAt ORDER BY c.certifiedAt")
  List<Object[]> getCustomerTrendsByDateForSales(
      @Param("salesPhone") String salesPhone,
      @Param("startDate") String startDate,
      @Param("endDate") String endDate);

  /** Get customer certification trends by month for specific sales person. */
  @Query(
      "SELECT DATE_FORMAT(c.certifiedAt, '%Y-%m') as month, COUNT(c) as count FROM Customer c WHERE c.deletedAt IS NULL AND c.salesPhone = :salesPhone AND c.certifiedAt IS NOT NULL AND c.certifiedAt BETWEEN :startDate AND :endDate GROUP BY DATE_FORMAT(c.certifiedAt, '%Y-%m') ORDER BY month")
  List<Object[]> getCustomerTrendsByMonthForSales(
      @Param("salesPhone") String salesPhone,
      @Param("startDate") String startDate,
      @Param("endDate") String endDate);

  /** Get customers created before specific date. */
  @Query("SELECT COUNT(c) FROM Customer c WHERE c.deletedAt IS NULL AND c.createdAt < :date")
  long countCustomersCreatedBefore(@Param("date") ZonedDateTime date);

  /** Debug: Get sample certified dates from database. */
  @Query(
      "SELECT c.certifiedAt FROM Customer c WHERE c.deletedAt IS NULL AND c.certifiedAt IS NOT NULL ORDER BY c.certifiedAt DESC LIMIT 20")
  java.util.List<String> findSampleCertifiedDates();

  /** Debug: Get minimum certified date. */
  @Query(
      "SELECT MIN(c.certifiedAt) FROM Customer c WHERE c.deletedAt IS NULL AND c.certifiedAt IS NOT NULL")
  String findMinCertifiedDate();

  /** Debug: Get maximum certified date. */
  @Query(
      "SELECT MAX(c.certifiedAt) FROM Customer c WHERE c.deletedAt IS NULL AND c.certifiedAt IS NOT NULL")
  String findMaxCertifiedDate();

  /** Get customers created before specific date for sales person. */
  @Query(
      "SELECT COUNT(c) FROM Customer c WHERE c.deletedAt IS NULL AND c.salesPhone = :salesPhone AND c.createdAt < :date")
  long countCustomersCreatedBeforeForSales(
      @Param("salesPhone") String salesPhone, @Param("date") ZonedDateTime date);

  /** Get customer certification trends by certificate type and date. */
  @Query(
      "SELECT c.certifiedAt as date, c.certificateType as type, COUNT(c) as count FROM Customer c WHERE c.deletedAt IS NULL AND c.certifiedAt IS NOT NULL AND c.certifiedAt BETWEEN :startDate AND :endDate GROUP BY c.certifiedAt, c.certificateType ORDER BY c.certifiedAt, c.certificateType")
  List<Object[]> getCustomerTrendsByCertificateType(
      @Param("startDate") String startDate, @Param("endDate") String endDate);

  /** Get customer certification trends by certificate type and date for specific sales person. */
  @Query(
      "SELECT c.certifiedAt as date, c.certificateType as type, COUNT(c) as count FROM Customer c WHERE c.deletedAt IS NULL AND c.salesPhone = :salesPhone AND c.certifiedAt IS NOT NULL AND c.certifiedAt BETWEEN :startDate AND :endDate GROUP BY c.certifiedAt, c.certificateType ORDER BY c.certifiedAt, c.certificateType")
  List<Object[]> getCustomerTrendsByCertificateTypeForSales(
      @Param("salesPhone") String salesPhone,
      @Param("startDate") String startDate,
      @Param("endDate") String endDate);

  /** Get customer certification trends by certificate type and month. */
  @Query(
      "SELECT DATE_FORMAT(c.certifiedAt, '%Y-%m') as month, c.certificateType as type, COUNT(c) as count FROM Customer c WHERE c.deletedAt IS NULL AND c.certifiedAt IS NOT NULL AND c.certifiedAt BETWEEN :startDate AND :endDate GROUP BY DATE_FORMAT(c.certifiedAt, '%Y-%m'), c.certificateType ORDER BY month, c.certificateType")
  List<Object[]> getCustomerTrendsByCertificateTypeByMonth(
      @Param("startDate") String startDate, @Param("endDate") String endDate);

  /** Get customer certification trends by certificate type and month for specific sales person. */
  @Query(
      "SELECT DATE_FORMAT(c.certifiedAt, '%Y-%m') as month, c.certificateType as type, COUNT(c) as count FROM Customer c WHERE c.deletedAt IS NULL AND c.salesPhone = :salesPhone AND c.certifiedAt IS NOT NULL AND c.certifiedAt BETWEEN :startDate AND :endDate GROUP BY DATE_FORMAT(c.certifiedAt, '%Y-%m'), c.certificateType ORDER BY month, c.certificateType")
  List<Object[]> getCustomerTrendsByCertificateTypeByMonthForSales(
      @Param("salesPhone") String salesPhone,
      @Param("startDate") String startDate,
      @Param("endDate") String endDate);
}
