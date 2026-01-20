package com.example.customers.repository;

import com.example.customers.model.CustomerDeleteRequest;
import com.example.customers.model.DeleteRequestStatus;
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
 * Repository interface for CustomerDeleteRequest entity.
 *
 * <p>Provides CRUD operations and queries for customer delete requests.
 */
@Repository
public interface CustomerDeleteRequestRepository
    extends JpaRepository<CustomerDeleteRequest, UUID> {

  /**
   * Find all pending delete requests with pagination.
   *
   * @param requestStatus the status of requests to find
   * @param pageable pagination parameters
   * @return page of delete requests with the specified status
   */
  Page<CustomerDeleteRequest> findByRequestStatus(
      DeleteRequestStatus requestStatus, Pageable pageable);

  /**
   * Find all delete requests made by a specific user.
   *
   * @param requestedById the ID of the user who made the request
   * @param pageable pagination parameters
   * @return page of delete requests made by the user
   */
  @Query("SELECT dr FROM CustomerDeleteRequest dr WHERE dr.requestedBy.id = :requestedById")
  Page<CustomerDeleteRequest> findByRequestedById(
      @Param("requestedById") UUID requestedById, Pageable pageable);

  /**
   * Find delete request by customer ID.
   *
   * @param customerId the ID of the customer
   * @return optional delete request for the customer
   */
  @Query("SELECT dr FROM CustomerDeleteRequest dr WHERE dr.customer.id = :customerId")
  Optional<CustomerDeleteRequest> findByCustomerId(@Param("customerId") UUID customerId);

  /**
   * Find all pending delete requests for a specific customer.
   *
   * @param customerId the ID of the customer
   * @return list of pending delete requests
   */
  @Query(
      "SELECT dr FROM CustomerDeleteRequest dr WHERE dr.customer.id = :customerId AND dr.requestStatus = :status")
  List<CustomerDeleteRequest> findPendingByCustomerIdAndStatus(
      @Param("customerId") UUID customerId, @Param("status") DeleteRequestStatus status);

  /**
   * Count pending delete requests.
   *
   * @return count of pending requests
   */
  long countByRequestStatus(DeleteRequestStatus requestStatus);
}
