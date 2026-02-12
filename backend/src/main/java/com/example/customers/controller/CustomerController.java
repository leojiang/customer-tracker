package com.example.customers.controller;

import com.example.customers.dto.common.ErrorResponse;
import com.example.customers.dto.common.ValidationResponse;
import com.example.customers.dto.customer.CustomerCreateRequest;
import com.example.customers.dto.customer.CustomerPageResponse;
import com.example.customers.dto.customer.CustomerUpdateRequest;
import com.example.customers.dto.customer.StatusTransitionRequest;
import com.example.customers.model.CertificateType;
import com.example.customers.model.Customer;
import com.example.customers.model.CustomerStatus;
import com.example.customers.model.CustomerType;
import com.example.customers.model.Sales;
import com.example.customers.model.SalesRole;
import com.example.customers.model.StatusHistory;
import com.example.customers.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for customer management operations.
 *
 * <p>Provides endpoints for CRUD operations, status transitions, and search functionality.
 */
@Tag(
    name = "Customer Management",
    description = "APIs for managing customers, status transitions, and history tracking")
@RestController
@RequestMapping("/api/customers")
@CrossOrigin(origins = "*")
public class CustomerController {

  private final CustomerService customerService;

  /**
   * Constructor for CustomerController.
   *
   * @param customerService service for customer operations
   */
  @Autowired
  public CustomerController(CustomerService customerService) {
    this.customerService = customerService;
  }

  @Operation(
      summary = "List customers with search and pagination",
      description =
          "Retrieve a paginated list of customers with optional search filters "
              + "by name, phone, company, or status")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved customers",
            content = @Content(schema = @Schema(implementation = CustomerPageResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters")
      })
  @GetMapping
  @PreAuthorize("hasAnyAuthority('ADMIN', 'OFFICER', 'CUSTOMER_AGENT')")
  public ResponseEntity<CustomerPageResponse> getCustomers(
      @Parameter(description = "Search query for customer name (case-insensitive)")
          @RequestParam(required = false)
          String q,
      @Parameter(description = "Search by phone number (partial match)")
          @RequestParam(required = false)
          String phone,
      @Parameter(description = "Filter by certificate issuer (can be specified multiple times)")
          @RequestParam(required = false)
          List<String> certificateIssuer,
      @Parameter(description = "Search by customer agent name (case-insensitive)")
          @RequestParam(required = false)
          String customerAgent,
      @Parameter(description = "Include soft-deleted customers in results")
          @RequestParam(defaultValue = "false")
          boolean includeDeleted,
      @Parameter(description = "Filter by certificate type (can be specified multiple times)")
          @RequestParam(required = false)
          List<String> certificateType,
      @Parameter(description = "Filter by customer type") @RequestParam(required = false)
          String customerType,
      @Parameter(description = "Filter by status (can be specified multiple times)")
          @RequestParam(required = false)
          List<String> status,
      @Parameter(description = "Filter by certified date start (ISO format)")
          @RequestParam(required = false)
          String certifiedStartDate,
      @Parameter(description = "Filter by certified date end (ISO format)")
          @RequestParam(required = false)
          String certifiedEndDate,
      @Parameter(description = "Page number (1-based)") @RequestParam(defaultValue = "1") int page,
      @Parameter(description = "Number of items per page (max 100)")
          @RequestParam(defaultValue = "5")
          int limit) {

    // Validate pagination parameters
    if (page < 1) {
      page = 1;
    }
    if (limit < 1) {
      limit = 20;
    }
    if (limit > 100) {
      limit = 100; // Max limit as per plan
    }

    // Always sort by certificate time ASC, then by creation time DESC for consistent ordering
    Pageable pageable =
        PageRequest.of(
            page - 1,
            limit,
            Sort.by("certifiedAt")
                .ascending()
                .and(Sort.by("createdAt").descending())
                .and(Sort.by("id").ascending()));

    // Get current user's sales phone for filtering (non-admin users can only see their own
    // customers)
    String filterBySalesPhone = getCurrentUserSalesPhone();

    // Convert certificate type strings to enum list
    List<CertificateType> certificateTypeEnumList = null;
    if (certificateType != null && !certificateType.isEmpty()) {
      certificateTypeEnumList = new java.util.ArrayList<>();
      for (String certType : certificateType) {
        if (certType != null && !certType.trim().isEmpty()) {
          try {
            certificateTypeEnumList.add(CertificateType.valueOf(certType.toUpperCase()));
          } catch (IllegalArgumentException e) {
            // Invalid certificate type, ignore
          }
        }
      }
    }

    // Convert customer type string to enum
    CustomerType customerTypeEnum = null;
    if (customerType != null && !customerType.trim().isEmpty()) {
      try {
        customerTypeEnum = CustomerType.valueOf(customerType.toUpperCase());
      } catch (IllegalArgumentException e) {
        // Invalid customer type, ignore
      }
    }

    // Convert certificate issuer strings to list (filter out null/empty values)
    List<String> certificateIssuerList = null;
    if (certificateIssuer != null && !certificateIssuer.isEmpty()) {
      certificateIssuerList = new java.util.ArrayList<>();
      for (String issuer : certificateIssuer) {
        if (issuer != null && !issuer.trim().isEmpty()) {
          certificateIssuerList.add(issuer.trim());
        }
      }
    }

    // Convert status strings to enum list
    List<CustomerStatus> customerStatuses = null;
    if (status != null && !status.isEmpty()) {
      customerStatuses = new java.util.ArrayList<>();
      for (String statusStr : status) {
        if (statusStr != null && !statusStr.trim().isEmpty()) {
          try {
            customerStatuses.add(CustomerStatus.valueOf(statusStr.toUpperCase()));
          } catch (IllegalArgumentException e) {
            // Invalid status, ignore
          }
        }
      }
    }

    Page<Customer> customers =
        customerService.searchCustomers(
            q,
            phone,
            customerStatuses,
            certificateIssuerList,
            filterBySalesPhone,
            includeDeleted,
            certificateTypeEnumList,
            customerAgent,
            customerTypeEnum,
            certifiedStartDate,
            certifiedEndDate,
            pageable);

    CustomerPageResponse response =
        new CustomerPageResponse(
            customers.getContent(),
            customers.getTotalElements(),
            page,
            limit,
            customers.getTotalPages());

    return ResponseEntity.ok(response);
  }

  @Operation(
      summary = "Create a new customer",
      description =
          "Create a new customer with the provided information. " + "Phone number must be unique.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "201",
            description = "Customer created successfully",
            content = @Content(schema = @Schema(implementation = Customer.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid input data or duplicate phone number")
      })
  @PostMapping
  @PreAuthorize("hasAnyAuthority('ADMIN', 'CUSTOMER_AGENT')")
  public ResponseEntity<Customer> createCustomer(
      @Parameter(description = "Customer information", required = true) @Valid @RequestBody
          CustomerCreateRequest request) {
    try {
      Customer customer = new Customer();
      customer.setName(request.getName());
      customer.setPhone(request.getPhone());
      customer.setCertificateIssuer(request.getCertificateIssuer());
      customer.setBusinessRequirements(request.getBusinessRequirements());
      customer.setCertificateType(request.getCertificateType());
      customer.setAge(request.getAge());
      customer.setEducation(request.getEducation());
      customer.setGender(request.getGender());
      customer.setAddress(request.getAddress());
      customer.setIdCard(request.getIdCard());
      customer.setCustomerAgent(request.getCustomerAgent());

      // Set certifiedAt directly as string (format: YYYY-MM-DD)
      customer.setCertifiedAt(request.getCertifiedAt());

      if (request.getCurrentStatus() != null) {
        customer.setCurrentStatus(request.getCurrentStatus());
      }

      // Get current user's phone to associate with the customer
      String currentUserPhone = getCurrentUserPhone();
      String currentUserName = getCurrentUserName();
      Customer savedCustomer =
          customerService.createCustomer(customer, currentUserPhone, currentUserName);
      return ResponseEntity.status(HttpStatus.CREATED).body(savedCustomer);

    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().build();
    }
  }

  /** GET /api/customers/:id Get customer by ID. */
  @GetMapping("/{id}")
  @PreAuthorize("hasAnyAuthority('ADMIN', 'OFFICER', 'CUSTOMER_AGENT')")
  public ResponseEntity<Customer> getCustomer(@PathVariable UUID id) {
    Optional<Customer> customer = customerService.getCustomerById(id);

    if (customer.isEmpty()) {
      return ResponseEntity.notFound().build();
    }

    // Check if user has access to this customer
    if (!hasAccessToCustomer(customer.get())) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    return ResponseEntity.ok(customer.get());
  }

  /** PATCH /api/customers/:id Update customer. */
  @PatchMapping("/{id}")
  @PreAuthorize("hasAnyAuthority('ADMIN', 'CUSTOMER_AGENT')")
  public ResponseEntity<Customer> updateCustomer(
      @PathVariable UUID id, @Valid @RequestBody CustomerUpdateRequest request) {
    try {
      // Check if customer exists and user has access
      Optional<Customer> existingCustomer = customerService.getCustomerById(id);
      if (existingCustomer.isEmpty() || !hasAccessToCustomer(existingCustomer.get())) {
        return ResponseEntity.notFound().build();
      }

      Customer customer = new Customer();
      customer.setName(request.getName());
      customer.setPhone(request.getPhone());
      customer.setCertificateIssuer(request.getCertificateIssuer());
      customer.setBusinessRequirements(request.getBusinessRequirements());
      customer.setCertificateType(request.getCertificateType());
      customer.setAge(request.getAge());
      customer.setEducation(request.getEducation());
      customer.setGender(request.getGender());
      customer.setAddress(request.getAddress());
      customer.setIdCard(request.getIdCard());
      customer.setCustomerAgent(request.getCustomerAgent());

      // Set certifiedAt directly as string (format: YYYY-MM-DD)
      customer.setCertifiedAt(request.getCertifiedAt());

      Customer updatedCustomer = customerService.updateCustomer(id, customer);
      return ResponseEntity.ok(updatedCustomer);

    } catch (EntityNotFoundException e) {
      return ResponseEntity.notFound().build();
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().build();
    }
  }

  /** DELETE /api/customers/:id Soft delete customer. */
  @DeleteMapping("/{id}")
  @PreAuthorize("hasAuthority('ADMIN')")
  public ResponseEntity<Void> deleteCustomer(@PathVariable UUID id) {
    try {
      // Check if customer exists and user has access
      Optional<Customer> customer = customerService.getCustomerById(id);
      if (customer.isEmpty() || !hasAccessToCustomer(customer.get())) {
        return ResponseEntity.notFound().build();
      }

      // Get current user's name for tracking
      String currentUserName = getCurrentUserName();
      customerService.deleteCustomer(id, currentUserName);
      return ResponseEntity.noContent().build();
    } catch (EntityNotFoundException e) {
      return ResponseEntity.notFound().build();
    }
  }

  /** POST /api/customers/:id/restore Restore soft-deleted customer. */
  @PostMapping("/{id}/restore")
  @PreAuthorize("hasAuthority('ADMIN')")
  public ResponseEntity<Customer> restoreCustomer(@PathVariable UUID id) {
    try {
      // Check if customer exists and user has access (need to use includeDeleted version)
      Optional<Customer> customer = customerService.getCustomerByIdIncludingDeleted(id);
      if (customer.isEmpty() || !hasAccessToCustomer(customer.get())) {
        return ResponseEntity.notFound().build();
      }

      // Get current user's name for tracking
      String currentUserName = getCurrentUserName();
      Customer restoredCustomer = customerService.restoreCustomer(id, currentUserName);
      return ResponseEntity.ok(restoredCustomer);
    } catch (EntityNotFoundException e) {
      return ResponseEntity.notFound().build();
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().build();
    }
  }

  @Operation(
      summary = "Transition customer status",
      description =
          "Change the status of a customer and record the transition in history "
              + "with an optional reason")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Status transitioned successfully",
            content = @Content(schema = @Schema(implementation = Customer.class))),
        @ApiResponse(responseCode = "404", description = "Customer not found"),
        @ApiResponse(responseCode = "400", description = "Invalid status transition")
      })
  @PostMapping("/{id}/status-transition")
  @PreAuthorize("hasAnyAuthority('ADMIN', 'CUSTOMER_AGENT')")
  public ResponseEntity<Customer> transitionStatus(
      @Parameter(description = "Customer ID", required = true) @PathVariable UUID id,
      @Parameter(description = "Status transition details", required = true) @Valid @RequestBody
          StatusTransitionRequest request) {
    try {
      // Check if customer exists and user has access
      Optional<Customer> customer = customerService.getCustomerById(id);
      if (customer.isEmpty() || !hasAccessToCustomer(customer.get())) {
        return ResponseEntity.notFound().build();
      }

      // Get current user's name for tracking
      String currentUserName = getCurrentUserName();
      Customer updatedCustomer =
          customerService.transitionStatus(
              id, request.getToStatus(), request.getReason(), currentUserName);
      return ResponseEntity.ok(updatedCustomer);
    } catch (EntityNotFoundException e) {
      return ResponseEntity.notFound().build();
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().build();
    }
  }

  /** GET /api/customers/:id/status-history Get status history for customer. */
  @GetMapping("/{id}/status-history")
  public ResponseEntity<List<StatusHistory>> getStatusHistory(
      @PathVariable UUID id,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int limit) {
    try {
      // Check if customer exists and user has access
      Optional<Customer> customer = customerService.getCustomerById(id);
      if (customer.isEmpty() || !hasAccessToCustomer(customer.get())) {
        return ResponseEntity.notFound().build();
      }

      if (limit <= 0) {
        // Return all history if no pagination requested
        List<StatusHistory> history = customerService.getCustomerStatusHistory(id);
        return ResponseEntity.ok(history);
      } else {
        // Return paginated history
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), Math.min(limit, 100));
        Page<StatusHistory> history = customerService.getCustomerStatusHistory(id, pageable);
        return ResponseEntity.ok(history.getContent());
      }
    } catch (EntityNotFoundException e) {
      return ResponseEntity.notFound().build();
    }
  }

  /** GET /api/customers/statistics Get customer statistics. */
  @GetMapping("/statistics")
  public ResponseEntity<CustomerService.CustomerStatistics> getStatistics(
      @RequestParam(defaultValue = "false") boolean includeDeleted) {
    CustomerService.CustomerStatistics stats =
        customerService.getCustomerStatistics(includeDeleted);
    return ResponseEntity.ok(stats);
  }

  /** GET /api/customers/recent Get recently updated customers. */
  @GetMapping("/recent")
  public ResponseEntity<CustomerPageResponse> getRecentCustomers(
      @RequestParam(defaultValue = "7") int days,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int limit) {

    if (page < 1) {
      page = 1;
    }
    if (limit < 1) {
      limit = 20;
    }
    if (limit > 100) {
      limit = 100;
    }

    Pageable pageable = PageRequest.of(page - 1, limit);
    Page<Customer> customers = customerService.getRecentlyUpdatedCustomers(days, pageable);

    CustomerPageResponse response =
        new CustomerPageResponse(
            customers.getContent(),
            customers.getTotalElements(),
            page,
            limit,
            customers.getTotalPages());

    return ResponseEntity.ok(response);
  }

  @Operation(
      summary = "Get valid status transitions",
      description =
          "Get all valid status transitions for a specific customer based on "
              + "current status and business rules")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Valid transitions retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Customer not found")
      })
  @GetMapping("/{id}/valid-transitions")
  public ResponseEntity<Set<CustomerStatus>> getValidTransitions(
      @Parameter(description = "Customer ID", required = true) @PathVariable UUID id) {
    try {
      Optional<Customer> customer = customerService.getCustomerById(id);
      if (customer.isEmpty() || !hasAccessToCustomer(customer.get())) {
        return ResponseEntity.notFound().build();
      }

      Set<CustomerStatus> validTransitions = customerService.getValidTransitions(id);
      return ResponseEntity.ok(validTransitions);
    } catch (EntityNotFoundException e) {
      return ResponseEntity.notFound().build();
    }
  }

  @Operation(
      summary = "Validate status transition",
      description = "Check if a specific status transition is valid for a customer")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Validation result returned"),
        @ApiResponse(responseCode = "404", description = "Customer not found")
      })
  @GetMapping("/{id}/can-transition-to/{status}")
  public ResponseEntity<ValidationResponse> canTransitionTo(
      @Parameter(description = "Customer ID", required = true) @PathVariable UUID id,
      @Parameter(description = "Target status", required = true) @PathVariable
          CustomerStatus status) {
    try {
      Optional<Customer> customer = customerService.getCustomerById(id);
      if (customer.isEmpty() || !hasAccessToCustomer(customer.get())) {
        return ResponseEntity.notFound().build();
      }

      boolean canTransition = customerService.isValidTransition(id, status);
      return ResponseEntity.ok(new ValidationResponse(canTransition));
    } catch (EntityNotFoundException e) {
      return ResponseEntity.notFound().build();
    }
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException e) {
    return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
  }

  @ExceptionHandler(EntityNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleEntityNotFound(EntityNotFoundException e) {
    return ResponseEntity.notFound().build();
  }

  // Helper methods for authorization
  private Sales getCurrentUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null && authentication.getPrincipal() instanceof Sales) {
      return (Sales) authentication.getPrincipal();
    }
    throw new IllegalStateException("No authenticated user found");
  }

  private String getCurrentUserPhone() {
    return getCurrentUser().getPhone();
  }

  private String getCurrentUserName() {
    return getCurrentUser().getName();
  }

  private String getCurrentUserSalesPhone() {
    Sales currentUser = getCurrentUser();
    // Admin, OFFICER, and CUSTOMER_AGENT can see all customers (no filter), regular sales can only
    // see their own
    return (currentUser.getRole() == SalesRole.ADMIN
            || currentUser.getRole() == SalesRole.OFFICER
            || currentUser.getRole() == SalesRole.CUSTOMER_AGENT)
        ? null
        : currentUser.getPhone();
  }

  private boolean hasAccessToCustomer(Customer customer) {
    Sales currentUser = getCurrentUser();
    // Admin, OFFICER, and CUSTOMER_AGENT have access to all customers
    if (currentUser.getRole() == SalesRole.ADMIN
        || currentUser.getRole() == SalesRole.OFFICER
        || currentUser.getRole() == SalesRole.CUSTOMER_AGENT) {
      return true;
    }
    // Regular sales can only access customers they created
    return currentUser.getPhone().equals(customer.getSalesPhone());
  }
}
