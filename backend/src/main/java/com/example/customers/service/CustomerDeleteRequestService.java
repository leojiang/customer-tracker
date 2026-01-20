package com.example.customers.service;

import com.example.customers.model.Customer;
import com.example.customers.model.CustomerDeleteRequest;
import com.example.customers.model.DeleteRequestStatus;
import com.example.customers.model.Sales;
import com.example.customers.repository.CustomerDeleteRequestRepository;
import com.example.customers.repository.CustomerRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * Service for managing customer delete requests.
 *
 * <p>Handles the workflow where officers request customer deletion and admins approve/reject
 * requests.
 */
@Service
public class CustomerDeleteRequestService {

  private final CustomerDeleteRequestRepository deleteRequestRepository;
  private final CustomerRepository customerRepository;

  /**
   * Constructor for CustomerDeleteRequestService.
   *
   * @param deleteRequestRepository repository for delete requests
   * @param customerRepository repository for customers
   */
  @Autowired
  public CustomerDeleteRequestService(
      CustomerDeleteRequestRepository deleteRequestRepository,
      CustomerRepository customerRepository) {
    this.deleteRequestRepository = deleteRequestRepository;
    this.customerRepository = customerRepository;
  }

  /**
   * Create a new delete request for a customer.
   *
   * @param customerId the ID of the customer to delete
   * @param requestedBy the sales user requesting deletion
   * @param reason the reason for deletion
   * @return the created delete request
   * @throws EntityNotFoundException if customer not found
   * @throws IllegalStateException if a pending request already exists
   */
  @Transactional
  public CustomerDeleteRequest createDeleteRequest(
      UUID customerId, Sales requestedBy, String reason) {
    Customer customer =
        customerRepository
            .findById(customerId)
            .orElseThrow(() -> new EntityNotFoundException("Customer not found with id: " + customerId));

    // Check if there's already a pending request for this customer
    java.util.List<CustomerDeleteRequest> existingPending =
        deleteRequestRepository.findPendingByCustomerIdAndStatus(
            customerId, DeleteRequestStatus.PENDING);

    if (!existingPending.isEmpty()) {
      throw new IllegalStateException(
          "A pending delete request already exists for this customer");
    }

    CustomerDeleteRequest deleteRequest = new CustomerDeleteRequest(customer, requestedBy, reason);
    return deleteRequestRepository.save(deleteRequest);
  }

  /**
   * Get all pending delete requests with pagination.
   *
   * @param pageable pagination parameters
   * @return page of pending delete requests
   */
  public Page<CustomerDeleteRequest> getPendingRequests(Pageable pageable) {
    return deleteRequestRepository.findByRequestStatus(DeleteRequestStatus.PENDING, pageable);
  }

  /**
   * Get delete requests by requester with pagination.
   *
   * @param requestedById the ID of the user who made the requests
   * @param pageable pagination parameters
   * @return page of delete requests
   */
  public Page<CustomerDeleteRequest> getRequestsByRequester(
      UUID requestedById, Pageable pageable) {
    return deleteRequestRepository.findByRequestedById(requestedById, pageable);
  }

  /**
   * Approve a delete request and delete the customer.
   *
   * @param requestId the ID of the delete request
   * @param adminPhone the phone number of the admin approving the request
   * @throws EntityNotFoundException if request not found
   * @throws IllegalStateException if request is not pending
   */
  @Transactional
  public void approveDeleteRequest(UUID requestId, String adminPhone) {
    CustomerDeleteRequest request =
        deleteRequestRepository
            .findById(requestId)
            .orElseThrow(
                () -> new EntityNotFoundException("Delete request not found with id: " + requestId));

    if (!request.isPending()) {
      throw new IllegalStateException("Cannot approve a request that is not pending");
    }

    // Approve the request
    request.approve(adminPhone);
    deleteRequestRepository.save(request);

    // Delete the customer (soft delete)
    Customer customer = request.getCustomer();
    customerRepository.delete(customer);
  }

  /**
   * Reject a delete request.
   *
   * @param requestId the ID of the delete request
   * @param adminPhone the phone number of the admin rejecting the request
   * @param rejectionReason the reason for rejection
   * @throws EntityNotFoundException if request not found
   * @throws IllegalStateException if request is not pending
   */
  @Transactional
  public void rejectDeleteRequest(UUID requestId, String adminPhone, String rejectionReason) {
    CustomerDeleteRequest request =
        deleteRequestRepository
            .findById(requestId)
            .orElseThrow(
                () -> new EntityNotFoundException("Delete request not found with id: " + requestId));

    if (!request.isPending()) {
      throw new IllegalStateException("Cannot reject a request that is not pending");
    }

    request.reject(adminPhone, rejectionReason);
    deleteRequestRepository.save(request);
  }

  /**
   * Get delete request by ID.
   *
   * @param requestId the ID of the delete request
   * @return the delete request
   * @throws EntityNotFoundException if request not found
   */
  public CustomerDeleteRequest getDeleteRequest(UUID requestId) {
    return deleteRequestRepository
        .findById(requestId)
        .orElseThrow(
            () -> new EntityNotFoundException("Delete request not found with id: " + requestId));
  }

  /**
   * Count pending delete requests.
   *
   * @return count of pending requests
   */
  public long countPendingRequests() {
    return deleteRequestRepository.countByRequestStatus(DeleteRequestStatus.PENDING);
  }
}
