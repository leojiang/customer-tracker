package com.example.customers.repository;

import com.example.customers.model.Customer;
import com.example.customers.model.CustomerStatus;
import com.example.customers.model.StatusHistory;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface StatusHistoryRepository extends JpaRepository<StatusHistory, UUID> {

  /** Find status history for a specific customer, ordered by most recent first */
  List<StatusHistory> findByCustomerOrderByChangedAtDesc(Customer customer);

  /** Find status history for a specific customer with pagination */
  Page<StatusHistory> findByCustomerOrderByChangedAtDesc(Customer customer, Pageable pageable);

  /** Find status history by customer ID */
  @Query(
      "SELECT sh FROM StatusHistory sh WHERE sh.customer.id = :customerId ORDER BY sh.changedAt DESC")
  List<StatusHistory> findByCustomerIdOrderByChangedAtDesc(@Param("customerId") UUID customerId);

  /** Find status history by customer ID with pagination */
  @Query(
      "SELECT sh FROM StatusHistory sh WHERE sh.customer.id = :customerId ORDER BY sh.changedAt DESC")
  Page<StatusHistory> findByCustomerIdOrderByChangedAtDesc(
      @Param("customerId") UUID customerId, Pageable pageable);

  /** Find latest status change for a customer */
  @Query(
      "SELECT sh FROM StatusHistory sh WHERE sh.customer.id = :customerId ORDER BY sh.changedAt DESC LIMIT 1")
  StatusHistory findLatestByCustomerId(@Param("customerId") UUID customerId);

  /** Count total status transitions for a customer */
  long countByCustomer(Customer customer);

  /** Find all transitions to a specific status */
  List<StatusHistory> findByToStatusOrderByChangedAtDesc(CustomerStatus toStatus);

  /** Find all transitions from a specific status */
  List<StatusHistory> findByFromStatusOrderByChangedAtDesc(CustomerStatus fromStatus);

  /** Find transitions between specific statuses */
  @Query(
      "SELECT sh FROM StatusHistory sh WHERE sh.fromStatus = :fromStatus AND sh.toStatus = :toStatus ORDER BY sh.changedAt DESC")
  List<StatusHistory> findByFromStatusAndToStatusOrderByChangedAtDesc(
      @Param("fromStatus") CustomerStatus fromStatus, @Param("toStatus") CustomerStatus toStatus);

  /**
   * Delete all status history for a customer (cascade should handle this, but explicit method for
   * testing)
   */
  void deleteByCustomer(Customer customer);
}
