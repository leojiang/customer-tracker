package com.example.customers.controller;

import com.example.customers.model.Customer;
import com.example.customers.model.CustomerStatus;
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
  public ResponseEntity<CustomerPageResponse> getCustomers(
      @Parameter(description = "Search query for customer name (case-insensitive)")
          @RequestParam(required = false)
          String q,
      @Parameter(description = "Search by phone number (partial match)")
          @RequestParam(required = false)
          String phone,
      @Parameter(description = "Filter by customer status") @RequestParam(required = false)
          CustomerStatus status,
      @Parameter(description = "Search by company name (case-insensitive)")
          @RequestParam(required = false)
          String company,
      @Parameter(description = "Include soft-deleted customers in results")
          @RequestParam(defaultValue = "false")
          boolean includeDeleted,
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

    // Convert to 0-based page for Spring Data
    Pageable pageable = PageRequest.of(page - 1, limit, Sort.by("updatedAt").descending());

    // Get current user's sales phone for filtering (non-admin users can only see their own
    // customers)
    String filterBySalesPhone = getCurrentUserSalesPhone();

    Page<Customer> customers =
        customerService.searchCustomers(
            q, phone, status, company, filterBySalesPhone, includeDeleted, pageable);

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
  public ResponseEntity<Customer> createCustomer(
      @Parameter(description = "Customer information", required = true) @Valid @RequestBody
          CreateCustomerRequest request) {
    try {
      Customer customer = new Customer();
      customer.setName(request.getName());
      customer.setPhone(request.getPhone());
      customer.setCompany(request.getCompany());
      customer.setBusinessRequirements(request.getBusinessRequirements());
      customer.setBusinessType(request.getBusinessType());
      customer.setAge(request.getAge());
      customer.setEducation(request.getEducation());
      customer.setGender(request.getGender());
      customer.setLocation(request.getLocation());

      if (request.getCurrentStatus() != null) {
        customer.setCurrentStatus(request.getCurrentStatus());
      }

      // Get current user's phone to associate with the customer
      String currentUserPhone = getCurrentUserPhone();
      Customer savedCustomer = customerService.createCustomer(customer, currentUserPhone);
      return ResponseEntity.status(HttpStatus.CREATED).body(savedCustomer);

    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().build();
    }
  }

  /** GET /api/customers/:id Get customer by ID. */
  @GetMapping("/{id}")
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
  public ResponseEntity<Customer> updateCustomer(
      @PathVariable UUID id, @Valid @RequestBody UpdateCustomerRequest request) {
    try {
      // Check if customer exists and user has access
      Optional<Customer> existingCustomer = customerService.getCustomerById(id);
      if (existingCustomer.isEmpty() || !hasAccessToCustomer(existingCustomer.get())) {
        return ResponseEntity.notFound().build();
      }

      Customer customer = new Customer();
      customer.setName(request.getName());
      customer.setPhone(request.getPhone());
      customer.setCompany(request.getCompany());
      customer.setBusinessRequirements(request.getBusinessRequirements());
      customer.setBusinessType(request.getBusinessType());
      customer.setAge(request.getAge());
      customer.setEducation(request.getEducation());
      customer.setGender(request.getGender());
      customer.setLocation(request.getLocation());

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
  public ResponseEntity<Void> deleteCustomer(@PathVariable UUID id) {
    try {
      // Check if customer exists and user has access
      Optional<Customer> customer = customerService.getCustomerById(id);
      if (customer.isEmpty() || !hasAccessToCustomer(customer.get())) {
        return ResponseEntity.notFound().build();
      }

      customerService.deleteCustomer(id);
      return ResponseEntity.noContent().build();
    } catch (EntityNotFoundException e) {
      return ResponseEntity.notFound().build();
    }
  }

  /** POST /api/customers/:id/restore Restore soft-deleted customer. */
  @PostMapping("/{id}/restore")
  public ResponseEntity<Customer> restoreCustomer(@PathVariable UUID id) {
    try {
      // Check if customer exists and user has access (need to use includeDeleted version)
      Optional<Customer> customer = customerService.getCustomerByIdIncludingDeleted(id);
      if (customer.isEmpty() || !hasAccessToCustomer(customer.get())) {
        return ResponseEntity.notFound().build();
      }

      Customer restoredCustomer = customerService.restoreCustomer(id);
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

      Customer updatedCustomer =
          customerService.transitionStatus(id, request.getToStatus(), request.getReason());
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
      // Check if customer exists and user has access
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
      // Check if customer exists and user has access
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

  // Exception handler for validation errors
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException e) {
    return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
  }

  @ExceptionHandler(EntityNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleEntityNotFound(EntityNotFoundException e) {
    return ResponseEntity.notFound().build();
  }

  // Request/Response DTOs
  /** Request DTO for creating a new customer. */
  public static class CreateCustomerRequest {
    private String name;
    private String phone;
    private String company;
    private String businessRequirements;
    private String businessType;
    private Integer age;
    private String education;
    private String gender;
    private String location;
    private CustomerStatus currentStatus;

    // Getters and setters
    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getPhone() {
      return phone;
    }

    public void setPhone(String phone) {
      this.phone = phone;
    }

    public String getCompany() {
      return company;
    }

    public void setCompany(String company) {
      this.company = company;
    }

    public String getBusinessRequirements() {
      return businessRequirements;
    }

    public void setBusinessRequirements(String businessRequirements) {
      this.businessRequirements = businessRequirements;
    }

    public String getBusinessType() {
      return businessType;
    }

    public void setBusinessType(String businessType) {
      this.businessType = businessType;
    }

    public Integer getAge() {
      return age;
    }

    public void setAge(Integer age) {
      this.age = age;
    }

    public String getEducation() {
      return education;
    }

    public void setEducation(String education) {
      this.education = education;
    }

    public String getGender() {
      return gender;
    }

    public void setGender(String gender) {
      this.gender = gender;
    }

    public String getLocation() {
      return location;
    }

    public void setLocation(String location) {
      this.location = location;
    }

    public CustomerStatus getCurrentStatus() {
      return currentStatus;
    }

    public void setCurrentStatus(CustomerStatus currentStatus) {
      this.currentStatus = currentStatus;
    }
  }

  /** Request DTO for updating customer information. */
  public static class UpdateCustomerRequest {
    private String name;
    private String phone;
    private String company;
    private String businessRequirements;
    private String businessType;
    private Integer age;
    private String education;
    private String gender;
    private String location;

    // Getters and setters
    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getPhone() {
      return phone;
    }

    public void setPhone(String phone) {
      this.phone = phone;
    }

    public String getCompany() {
      return company;
    }

    public void setCompany(String company) {
      this.company = company;
    }

    public String getBusinessRequirements() {
      return businessRequirements;
    }

    public void setBusinessRequirements(String businessRequirements) {
      this.businessRequirements = businessRequirements;
    }

    public String getBusinessType() {
      return businessType;
    }

    public void setBusinessType(String businessType) {
      this.businessType = businessType;
    }

    public Integer getAge() {
      return age;
    }

    public void setAge(Integer age) {
      this.age = age;
    }

    public String getEducation() {
      return education;
    }

    public void setEducation(String education) {
      this.education = education;
    }

    public String getGender() {
      return gender;
    }

    public void setGender(String gender) {
      this.gender = gender;
    }

    public String getLocation() {
      return location;
    }

    public void setLocation(String location) {
      this.location = location;
    }
  }

  /** Request DTO for customer status transitions. */
  public static class StatusTransitionRequest {
    private CustomerStatus toStatus;
    private String reason;

    public CustomerStatus getToStatus() {
      return toStatus;
    }

    public void setToStatus(CustomerStatus toStatus) {
      this.toStatus = toStatus;
    }

    public String getReason() {
      return reason;
    }

    public void setReason(String reason) {
      this.reason = reason;
    }
  }

  /** Response DTO for paginated customer lists. */
  public static class CustomerPageResponse {
    private List<Customer> items;
    private long total;
    private int page;
    private int limit;
    private int totalPages;

    /**
     * Constructor for CustomerPageResponse.
     *
     * @param items list of customers
     * @param total total number of customers
     * @param page current page number
     * @param limit items per page
     * @param totalPages total number of pages
     */
    public CustomerPageResponse(
        List<Customer> items, long total, int page, int limit, int totalPages) {
      this.items = items;
      this.total = total;
      this.page = page;
      this.limit = limit;
      this.totalPages = totalPages;
    }

    public List<Customer> getItems() {
      return items;
    }

    public long getTotal() {
      return total;
    }

    public int getPage() {
      return page;
    }

    public int getLimit() {
      return limit;
    }

    public int getTotalPages() {
      return totalPages;
    }
  }

  /** Response DTO for error messages. */
  public static class ErrorResponse {
    private String message;

    public ErrorResponse(String message) {
      this.message = message;
    }

    public String getMessage() {
      return message;
    }
  }

  /** Response DTO for validation results. */
  public static class ValidationResponse {
    private boolean valid;

    public ValidationResponse(boolean valid) {
      this.valid = valid;
    }

    public boolean isValid() {
      return valid;
    }
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

  private String getCurrentUserSalesPhone() {
    Sales currentUser = getCurrentUser();
    // Admin can see all customers (no filter), regular sales can only see their own
    return currentUser.getRole() == SalesRole.ADMIN ? null : currentUser.getPhone();
  }

  private boolean hasAccessToCustomer(Customer customer) {
    Sales currentUser = getCurrentUser();
    // Admin has access to all customers
    if (currentUser.getRole() == SalesRole.ADMIN) {
      return true;
    }
    // Regular sales can only access customers they created
    return currentUser.getPhone().equals(customer.getSalesPhone());
  }
}
