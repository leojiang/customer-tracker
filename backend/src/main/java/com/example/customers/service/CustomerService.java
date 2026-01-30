package com.example.customers.service;

import com.example.customers.exception.BusinessException;
import com.example.customers.model.CertificateType;
import com.example.customers.model.Customer;
import com.example.customers.model.CustomerStatus;
import com.example.customers.model.StatusHistory;
import com.example.customers.repository.CustomerRepository;
import com.example.customers.repository.CustomerSpecifications;
import com.example.customers.repository.MonthlyCertifiedCountByCertificateTypeRepository;
import com.example.customers.repository.MonthlyCertifiedCountRepository;
import com.example.customers.repository.StatusHistoryRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service class for customer operations.
 *
 * <p>Provides business logic for customer management, status transitions, and search operations.
 */
@Service
@Transactional
public class CustomerService {

  private final CustomerRepository customerRepository;
  private final StatusHistoryRepository statusHistoryRepository;
  private final MonthlyCertifiedCountRepository monthlyCertifiedCountRepository;
  private final MonthlyCertifiedCountByCertificateTypeRepository
      monthlyCertifiedCountByCertificateTypeRepository;
  private final StatusTransitionValidator transitionValidator;

  /**
   * Constructor for CustomerService.
   *
   * @param customerRepository customer data repository
   * @param statusHistoryRepository status history repository
   * @param monthlyCertifiedCountRepository monthly certified count repository
   * @param monthlyCertifiedCountByCertificateTypeRepository monthly certified count by certificate
   *     type repository
   * @param transitionValidator status transition validator
   */
  @Autowired
  public CustomerService(
      CustomerRepository customerRepository,
      StatusHistoryRepository statusHistoryRepository,
      MonthlyCertifiedCountRepository monthlyCertifiedCountRepository,
      MonthlyCertifiedCountByCertificateTypeRepository
          monthlyCertifiedCountByCertificateTypeRepository,
      StatusTransitionValidator transitionValidator) {
    this.customerRepository = customerRepository;
    this.statusHistoryRepository = statusHistoryRepository;
    this.monthlyCertifiedCountRepository = monthlyCertifiedCountRepository;
    this.monthlyCertifiedCountByCertificateTypeRepository =
        monthlyCertifiedCountByCertificateTypeRepository;
    this.transitionValidator = transitionValidator;
  }

  /** Create a new customer. */
  public Customer createCustomer(Customer customer) {
    // Validate composite uniqueness (id_card, certificate_type)
    if (customer.getIdCard() != null && customer.getCertificateType() != null) {
      Optional<Customer> existingCustomer =
          customerRepository.findByIdCardAndCertificateType(
              customer.getIdCard(), customer.getCertificateType());

      if (existingCustomer.isPresent()) {
        throw new BusinessException(
            BusinessException.ErrorCode.DUPLICATE_CUSTOMER_CERTIFICATE,
            "A customer with ID card '"
                + customer.getIdCard()
                + "' already has a '"
                + customer.getCertificateType()
                + "' certificate. "
                + "Each combination of ID card and certificate type must be unique. "
                + "Please edit the existing customer or choose a different certificate type.");
      }
    }

    // Set default status if not provided
    if (customer.getCurrentStatus() == null) {
      customer.setCurrentStatus(CustomerStatus.NEW);
    }

    // Set default customer type if not provided (already defaulted in entity, but being explicit)
    if (customer.getCustomerType() == null) {
      customer.setCustomerType(com.example.customers.model.CustomerType.NEW_CUSTOMER);
    }

    Customer savedCustomer = customerRepository.save(customer);

    // Create initial status history
    createStatusHistory(
        savedCustomer, null, savedCustomer.getCurrentStatus(), "Initial customer creation");

    return savedCustomer;
  }

  /** Create a new customer with sales person assignment. */
  public Customer createCustomer(Customer customer, String salesPhone) {
    customer.setSalesPhone(salesPhone);
    return createCustomer(customer);
  }

  /** Get customer by ID (active only). */
  @Transactional(readOnly = true)
  public Optional<Customer> getCustomerById(UUID id) {
    return customerRepository.findById(id);
  }

  /** Get customer by ID including soft-deleted. */
  @Transactional(readOnly = true)
  public Optional<Customer> getCustomerByIdIncludingDeleted(UUID id) {
    return customerRepository.findByIdIncludingDeleted(id);
  }

  /** Update customer information. */
  public Customer updateCustomer(UUID id, Customer updatedCustomer) {
    Customer existingCustomer =
        customerRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Customer not found with id: " + id));

    // Check if ID card is being changed and validate uniqueness
    boolean idCardChanged =
        !java.util.Objects.equals(existingCustomer.getIdCard(), updatedCustomer.getIdCard());

    // Check if certificate type is being changed
    boolean certificateTypeChanged =
        !java.util.Objects.equals(
            existingCustomer.getCertificateType(), updatedCustomer.getCertificateType());

    // Check composite uniqueness (id_card, certificate_type) if either is being changed
    if ((idCardChanged || certificateTypeChanged)
        && updatedCustomer.getIdCard() != null
        && updatedCustomer.getCertificateType() != null) {

      Optional<Customer> duplicateCustomer =
          customerRepository.findByIdCardAndCertificateType(
              updatedCustomer.getIdCard(), updatedCustomer.getCertificateType());

      if (duplicateCustomer.isPresent() && !duplicateCustomer.get().getId().equals(id)) {
        throw new BusinessException(
            BusinessException.ErrorCode.DUPLICATE_CUSTOMER_CERTIFICATE,
            "A customer with ID card '"
                + updatedCustomer.getIdCard()
                + "' already has a '"
                + updatedCustomer.getCertificateType()
                + "' certificate. "
                + "Each combination of ID card and certificate type must be unique. "
                + "Please edit the existing customer or choose a different certificate type.");
      }
    }

    // Update fields (preserve system fields)
    existingCustomer.setName(updatedCustomer.getName());
    existingCustomer.setPhone(updatedCustomer.getPhone());
    existingCustomer.setCertificateIssuer(updatedCustomer.getCertificateIssuer());
    existingCustomer.setBusinessRequirements(updatedCustomer.getBusinessRequirements());
    existingCustomer.setCertificateType(updatedCustomer.getCertificateType());
    existingCustomer.setAge(updatedCustomer.getAge());
    existingCustomer.setEducation(updatedCustomer.getEducation());
    existingCustomer.setGender(updatedCustomer.getGender());
    existingCustomer.setAddress(updatedCustomer.getAddress());
    existingCustomer.setIdCard(updatedCustomer.getIdCard());
    existingCustomer.setCertifiedAt(updatedCustomer.getCertifiedAt());
    existingCustomer.setCustomerAgent(updatedCustomer.getCustomerAgent());

    return customerRepository.save(existingCustomer);
  }

  /** Transition customer status with history tracking and validation. */
  @Transactional
  public Customer transitionStatus(UUID customerId, CustomerStatus toStatus, String reason) {
    Customer customer =
        customerRepository
            .findById(customerId)
            .orElseThrow(
                () -> new EntityNotFoundException("Customer not found with id: " + customerId));

    CustomerStatus fromStatus = customer.getCurrentStatus();

    // Allow no-op transition (same status) - return customer without changes
    if (fromStatus.equals(toStatus)) {
      return customer;
    }

    // Validate status transition according to business rules
    if (!transitionValidator.isValidTransition(fromStatus, toStatus)) {
      String errorMessage = transitionValidator.getTransitionErrorMessage(fromStatus, toStatus);
      throw new IllegalArgumentException(errorMessage);
    }

    // Update customer status
    customer.setCurrentStatus(toStatus);

    // Set certifiedAt to current date when transitioning to CERTIFIED status
    if (toStatus == CustomerStatus.CERTIFIED) {
      customer.setCertifiedAt(java.time.LocalDate.now().toString());

      // Increment monthly certified count
      String month = extractMonthFromDate(customer.getCertifiedAt());
      monthlyCertifiedCountRepository.incrementCertifiedCount(month);

      // Increment monthly certified count by certificate type
      String certificateType =
          customer.getCertificateType() != null ? customer.getCertificateType().name() : "OTHER";
      monthlyCertifiedCountByCertificateTypeRepository.incrementCertifiedCount(
          month, certificateType);
    }

    Customer savedCustomer = customerRepository.save(customer);

    // Create status history
    createStatusHistory(savedCustomer, fromStatus, toStatus, reason);

    return savedCustomer;
  }

  /** Get status history for a customer. */
  @Transactional(readOnly = true)
  public List<StatusHistory> getCustomerStatusHistory(UUID customerId) {
    Customer customer =
        customerRepository
            .findById(customerId)
            .orElseThrow(
                () -> new EntityNotFoundException("Customer not found with id: " + customerId));

    return statusHistoryRepository.findByCustomerOrderByChangedAtDesc(customer);
  }

  /** Get paginated status history for a customer. */
  @Transactional(readOnly = true)
  public Page<StatusHistory> getCustomerStatusHistory(UUID customerId, Pageable pageable) {
    Customer customer =
        customerRepository
            .findById(customerId)
            .orElseThrow(
                () -> new EntityNotFoundException("Customer not found with id: " + customerId));

    return statusHistoryRepository.findByCustomerOrderByChangedAtDesc(customer, pageable);
  }

  /**
   * Search customers with pagination.
   *
   * <p>If only nameQuery is provided, use unified search across multiple fields.
   */
  @Transactional(readOnly = true)
  public Page<Customer> searchCustomers(
      String nameQuery,
      String phoneQuery,
      List<CustomerStatus> statuses,
      String certificateIssuer,
      String salesPhone,
      boolean includeDeleted,
      List<CertificateType> certificateTypes,
      String customerAgent,
      com.example.customers.model.CustomerType customerType,
      String certifiedStartDate,
      String certifiedEndDate,
      Pageable pageable) {
    Specification<Customer> spec;

    // Always use detailed search (only name and phone, as per requirements)
    spec =
        CustomerSpecifications.searchCustomers(
            nameQuery,
            phoneQuery,
            statuses,
            certificateIssuer,
            salesPhone,
            includeDeleted,
            certificateTypes,
            customerAgent,
            customerType,
            certifiedStartDate,
            certifiedEndDate);

    return customerRepository.findAll(spec, pageable);
  }

  /** Soft delete customer. */
  public void deleteCustomer(UUID id) {
    Customer customer =
        customerRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Customer not found with id: " + id));

    customer.softDelete();
    customerRepository.save(customer);

    // Create status history for deletion
    createStatusHistory(
        customer,
        customer.getCurrentStatus(),
        customer.getCurrentStatus(),
        "Customer soft deleted");
  }

  /** Restore soft-deleted customer. */
  public Customer restoreCustomer(UUID id) {
    Customer customer =
        customerRepository
            .findByIdIncludingDeleted(id)
            .orElseThrow(() -> new EntityNotFoundException("Customer not found with id: " + id));

    if (!customer.isDeleted()) {
      throw new IllegalArgumentException("Customer is not deleted");
    }

    customer.restore();
    Customer restoredCustomer = customerRepository.save(customer);

    // Create status history for restoration
    createStatusHistory(
        restoredCustomer,
        restoredCustomer.getCurrentStatus(),
        restoredCustomer.getCurrentStatus(),
        "Customer restored from soft delete");

    return restoredCustomer;
  }

  /** Get recently updated customers (within last N days). */
  @Transactional(readOnly = true)
  public Page<Customer> getRecentlyUpdatedCustomers(int days, Pageable pageable) {
    ZonedDateTime since = ZonedDateTime.now().minusDays(days);
    return customerRepository.findRecentlyUpdated(since, pageable);
  }

  /** Get customer statistics. */
  @Transactional(readOnly = true)
  public CustomerStatistics getCustomerStatistics(boolean includeDeleted) {
    CustomerStatistics stats = new CustomerStatistics();

    if (includeDeleted) {
      stats.setTotalCustomers(customerRepository.count());
      for (CustomerStatus status : CustomerStatus.values()) {
        stats.addStatusCount(
            status, customerRepository.countByCurrentStatusIncludingDeleted(status));
      }
    } else {
      stats.setTotalCustomers(customerRepository.count());
      for (CustomerStatus status : CustomerStatus.values()) {
        stats.addStatusCount(status, customerRepository.countByCurrentStatus(status));
      }
    }

    // Recent activity (last 7 days)
    ZonedDateTime weekAgo = ZonedDateTime.now().minusDays(7);
    Page<Customer> recentlyUpdated =
        customerRepository.findRecentlyUpdated(weekAgo, Pageable.unpaged());
    stats.setRecentlyUpdatedCount(recentlyUpdated.getTotalElements());

    return stats;
  }

  /** Check if phone number is available (not taken by any customer, including deleted). */
  @Transactional(readOnly = true)
  public boolean isPhoneAvailable(String phone) {
    return customerRepository.findByPhoneIncludingDeleted(phone).isEmpty();
  }

  /** Check if phone number is available for customer (excluding the customer's own record). */
  @Transactional(readOnly = true)
  public boolean isPhoneAvailableForCustomer(String phone, UUID customerId) {
    Optional<Customer> existing = customerRepository.findByPhoneIncludingDeleted(phone);
    return existing.isEmpty() || existing.get().getId().equals(customerId);
  }

  /** Get valid status transitions for a customer. */
  @Transactional(readOnly = true)
  public java.util.Set<CustomerStatus> getValidTransitions(UUID customerId) {
    Customer customer =
        customerRepository
            .findById(customerId)
            .orElseThrow(
                () -> new EntityNotFoundException("Customer not found with id: " + customerId));

    return transitionValidator.getValidTransitions(customer.getCurrentStatus());
  }

  /** Check if a status transition is valid for a customer. */
  @Transactional(readOnly = true)
  public boolean isValidTransition(UUID customerId, CustomerStatus toStatus) {
    Customer customer =
        customerRepository
            .findById(customerId)
            .orElseThrow(
                () -> new EntityNotFoundException("Customer not found with id: " + customerId));

    return transitionValidator.isValidTransition(customer.getCurrentStatus(), toStatus);
  }

  /** Get all certificates for a specific phone number. */
  @Transactional(readOnly = true)
  public List<Customer> getCertificatesByPhone(String phone) {
    return customerRepository.findAllByPhone(phone);
  }

  /** Get certificate by name, phone and certificate type. */
  @Transactional(readOnly = true)
  public Optional<Customer> getCertificateByNamePhoneAndType(
      String name, String phone, CertificateType certificateType) {
    return customerRepository.findByNameAndPhoneAndCertificateType(name, phone, certificateType);
  }

  /** Check if a customer with the same name, phone and certificate type already exists. */
  @Transactional(readOnly = true)
  public boolean existsByNamePhoneAndCertificateType(
      String name, String phone, CertificateType certificateType) {
    return customerRepository
        .findByNameAndPhoneAndCertificateType(name, phone, certificateType)
        .isPresent();
  }

  // Private helper methods

  private void createStatusHistory(
      Customer customer, CustomerStatus fromStatus, CustomerStatus toStatus, String reason) {
    StatusHistory history = new StatusHistory(customer, fromStatus, toStatus, reason);
    statusHistoryRepository.save(history);
  }

  /**
   * Extract month (yyyy-MM) from date string (yyyy-MM-dd).
   *
   * @param dateStr Date string in 'yyyy-MM-dd' format
   * @return Month in 'yyyy-MM' format
   * @throws IllegalArgumentException if date string is invalid
   */
  private String extractMonthFromDate(String dateStr) {
    if (dateStr == null || dateStr.length() < 7) {
      throw new IllegalArgumentException("Invalid date format: " + dateStr);
    }
    return dateStr.substring(0, 7); // Returns "yyyy-MM" from "yyyy-MM-dd"
  }

  // Inner class for statistics
  /** DTO class for customer statistics. */
  public static class CustomerStatistics {
    private long totalCustomers;
    private long recentlyUpdatedCount;
    private java.util.Map<CustomerStatus, Long> statusCounts = new java.util.HashMap<>();

    public long getTotalCustomers() {
      return totalCustomers;
    }

    public void setTotalCustomers(long totalCustomers) {
      this.totalCustomers = totalCustomers;
    }

    public long getRecentlyUpdatedCount() {
      return recentlyUpdatedCount;
    }

    public void setRecentlyUpdatedCount(long recentlyUpdatedCount) {
      this.recentlyUpdatedCount = recentlyUpdatedCount;
    }

    public java.util.Map<CustomerStatus, Long> getStatusCounts() {
      return statusCounts;
    }

    public void addStatusCount(CustomerStatus status, long count) {
      this.statusCounts.put(status, count);
    }
  }
}
